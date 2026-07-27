package com.pyin.plugin.exportworkshop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.exportworkshop.support.ExportWorkshopProperties;
import com.pyin.plugin.exportworkshop.web.WorkshopRequests;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExportWorkshopService {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of("xlsx", "json");
    private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\{\\{?\\s*([A-Za-z_][A-Za-z0-9_.-]*)\\s*\\}?\\}");
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ExportWorkshopProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public ExportWorkshopService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, ExportWorkshopProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_export_workshop_directory (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, parent_id BIGINT, name VARCHAR(128) NOT NULL,
                    deleted BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_export_workshop_template (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, directory_id BIGINT, name VARCHAR(255) NOT NULL,
                    source_type VARCHAR(32) NOT NULL, source_uri VARCHAR(2048), source_checksum VARCHAR(128),
                    source_etag VARCHAR(256), source_updated_at TIMESTAMP, parent_template_id BIGINT,
                    read_only BOOLEAN NOT NULL DEFAULT FALSE, workbook_snapshot CLOB NOT NULL, mappings_json CLOB,
                    original_file_path VARCHAR(1024), deleted BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_export_workshop_version (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, template_id BIGINT NOT NULL, version_no BIGINT NOT NULL,
                    workbook_snapshot CLOB NOT NULL, mappings_json CLOB, created_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_export_workshop_version UNIQUE (template_id, version_no))
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_export_workshop_export_task (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id VARCHAR(64) NOT NULL, template_id BIGINT,
                    file_name VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL, progress INT NOT NULL,
                    preview_image CLOB, output_path VARCHAR(1024), error_message VARCHAR(1024),
                    created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_export_workshop_export_task UNIQUE (task_id))
                """);
        try {
            Files.createDirectories(storagePath("sources"));
            Files.createDirectories(storagePath("exports"));
        } catch (IOException exception) {
            throw new IllegalStateException("无法初始化导出工坊数据目录", exception);
        }
    }

    public List<Map<String, Object>> listTree() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        jdbcTemplate.query("SELECT id, parent_id, name FROM pyin_export_workshop_directory WHERE deleted = FALSE ORDER BY name", rs -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", "directory-" + rs.getLong("id")); node.put("nodeType", "DIRECTORY");
            node.put("parentId", rs.getObject("parent_id") == null ? null : "directory-" + rs.getLong("parent_id"));
            node.put("name", rs.getString("name")); nodes.add(node);
        });
        jdbcTemplate.query("SELECT id, directory_id, name, source_type, read_only, parent_template_id, updated_at FROM pyin_export_workshop_template WHERE deleted = FALSE ORDER BY name", rs -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", "template-" + rs.getLong("id")); node.put("nodeType", "TEMPLATE");
            node.put("parentId", rs.getObject("directory_id") == null ? null : "directory-" + rs.getLong("directory_id"));
            node.put("name", rs.getString("name")); node.put("sourceType", rs.getString("source_type"));
            node.put("readOnly", rs.getBoolean("read_only")); node.put("parentTemplateId", rs.getObject("parent_template_id"));
            node.put("updatedAt", rs.getTimestamp("updated_at").toInstant().toString()); nodes.add(node);
        });
        return nodes;
    }

    @Transactional
    public Map<String, Object> createFolder(WorkshopRequests.FolderSaveRequest request) {
        String name = requireName(request.name()); validateParentDirectory(request.parentId());
        Instant now = Instant.now();
        jdbcTemplate.update("INSERT INTO pyin_export_workshop_directory (parent_id, name, created_at, updated_at) VALUES (?, ?, ?, ?)", request.parentId(), name, now, now);
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM pyin_export_workshop_directory", Long.class);
        return Map.of("id", id, "name", name);
    }

    @Transactional
    public Map<String, Object> createBlank(WorkshopRequests.BlankTemplateRequest request) {
        return createTemplate(request.directoryId(), requireName(request.name()), "ONLINE", null, false, defaultWorkbook(request.name()), "[]", null, null);
    }

    @Transactional
    public Map<String, Object> importFile(Long directoryId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("上传文件不能为空");
        if (file.getSize() > properties.getMaxFileSizeBytes()) throw invalid("模板文件超过大小限制");
        String name = requireName(stripExtension(file.getOriginalFilename())); String extension = extension(file.getOriginalFilename());
        validateExtension(extension);
        try {
            byte[] content = file.getBytes();
            Path target = storagePath("sources", UUID.randomUUID() + "." + extension);
            Files.write(target, content);
            String snapshot = "json".equals(extension) ? new String(content, java.nio.charset.StandardCharsets.UTF_8) : defaultWorkbook(name);
            validateWorkbookJson(snapshot);
            return createTemplate(directoryId, name, "UPLOAD", file.getOriginalFilename(), false, snapshot, "[]", target.toString(), sha256(content));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "读取上传模板失败");
        }
    }

    @Transactional
    public List<Map<String, Object>> mountLocalDirectory(WorkshopRequests.LocalDirectoryMountRequest request) {
        Path root = resolveConfiguredRoot(request.root()); validateParentDirectory(request.directoryId());
        try (var paths = Files.walk(root)) {
            List<Map<String, Object>> mounted = new ArrayList<>();
            paths.filter(Files::isRegularFile).filter(this::isSupported).sorted().forEach(path -> {
                try {
                    byte[] content = Files.readAllBytes(path);
                    if (content.length > properties.getMaxFileSizeBytes()) return;
                    String extension = extension(path.getFileName().toString());
                    String name = stripExtension(path.getFileName().toString());
                    String snapshot = "json".equals(extension) ? new String(content, java.nio.charset.StandardCharsets.UTF_8) : defaultWorkbook(name);
                    validateWorkbookJson(snapshot);
                    mounted.add(createTemplate(request.directoryId(), name, "LOCAL_DIRECTORY", path.toString(), true, snapshot, "[]", null, sha256(content)));
                } catch (IOException ignored) { }
            });
            return mounted;
        } catch (IOException exception) {
            throw invalid("无法扫描本地模板目录");
        }
    }

    @Transactional
    public Map<String, Object> mountNetwork(WorkshopRequests.NetworkMountRequest request) {
        URI uri = validateRemoteUri(request.url());
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw invalid("远程模板请求失败");
            byte[] content = response.body(); if (content.length > properties.getMaxFileSizeBytes()) throw invalid("远程模板超过大小限制");
            String extension = extension(uri.getPath()); validateExtension(extension);
            String name = StringUtils.hasText(request.name()) ? requireName(request.name()) : requireName(stripExtension(Path.of(uri.getPath()).getFileName().toString()));
            String snapshot = "json".equals(extension) ? new String(content, java.nio.charset.StandardCharsets.UTF_8) : defaultWorkbook(name);
            validateWorkbookJson(snapshot);
            return createTemplate(request.directoryId(), name, "NETWORK", uri.toString(), true, snapshot, "[]", null, sha256(content));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); throw invalid("远程模板请求被中断");
        } catch (IOException exception) {
            throw invalid("无法读取远程模板");
        }
    }

    public List<String> localRoots() { return properties.getLocalRoots().stream().filter(StringUtils::hasText).map(path -> Path.of(path).toAbsolutePath().normalize().toString()).toList(); }

    public Map<String, Object> readTemplate(Long id) { return template(id); }

    @Transactional
    public Map<String, Object> saveWorkbook(Long id, WorkshopRequests.WorkbookSaveRequest request) {
        Map<String, Object> current = template(id);
        if (Boolean.TRUE.equals(current.get("readOnly"))) return forkAndSave(id, request);
        String snapshot = serialize(request.workbookSnapshot()); validateWorkbookJson(snapshot);
        String mappings = serialize(request.mappings() == null ? List.of() : request.mappings()); Instant now = Instant.now();
        jdbcTemplate.update("UPDATE pyin_export_workshop_template SET name = ?, workbook_snapshot = ?, mappings_json = ?, updated_at = ? WHERE id = ?", requireName(request.name()), snapshot, mappings, now, id);
        long version = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_export_workshop_version WHERE template_id = ?", Long.class, id) + 1;
        jdbcTemplate.update("INSERT INTO pyin_export_workshop_version (template_id, version_no, workbook_snapshot, mappings_json, created_at) VALUES (?, ?, ?, ?, ?)", id, version, snapshot, mappings, now);
        return template(id);
    }

    @Transactional
    public Map<String, Object> rename(String nodeId, WorkshopRequests.RenameRequest request) {
        String name = requireName(request.name());
        if (nodeId.startsWith("directory-")) jdbcTemplate.update("UPDATE pyin_export_workshop_directory SET name = ?, updated_at = ? WHERE id = ? AND deleted = FALSE", name, Instant.now(), parseNodeId(nodeId));
        else if (nodeId.startsWith("template-")) {
            Long id = parseNodeId(nodeId); Map<String, Object> row = template(id);
            if (Boolean.TRUE.equals(row.get("readOnly"))) return forkAndSave(id, new WorkshopRequests.WorkbookSaveRequest(name, row.get("workbookSnapshot"), castMappings(row.get("mappings"))));
            jdbcTemplate.update("UPDATE pyin_export_workshop_template SET name = ?, updated_at = ? WHERE id = ?", name, Instant.now(), id);
        } else throw invalid("未知资源节点");
        return Map.of("name", name);
    }

    @Transactional
    public void delete(String nodeId) {
        Long id = parseNodeId(nodeId);
        if (nodeId.startsWith("directory-")) {
            jdbcTemplate.update("UPDATE pyin_export_workshop_directory SET deleted = TRUE, updated_at = ? WHERE id = ?", Instant.now(), id);
            jdbcTemplate.update("UPDATE pyin_export_workshop_template SET deleted = TRUE, updated_at = ? WHERE directory_id = ?", Instant.now(), id);
        } else if (nodeId.startsWith("template-")) jdbcTemplate.update("UPDATE pyin_export_workshop_template SET deleted = TRUE, updated_at = ? WHERE id = ?", Instant.now(), id);
        else throw invalid("未知资源节点");
    }

    @Transactional
    public Map<String, Object> fork(Long id) {
        Map<String, Object> row = template(id);
        return createTemplate(asLong(row.get("directoryId")), row.get("name") + "（副本）", "ONLINE", String.valueOf(row.get("sourceUri")), false,
                serialize(row.get("workbookSnapshot")), serialize(row.get("mappings")), null, String.valueOf(row.get("sourceChecksum")), id);
    }

    public Map<String, Object> debug(WorkshopRequests.DebugRequest request) {
        JsonNode mock = objectMapper.valueToTree(request.mockData());
        Map<String, Map<String, Object>> changes = new LinkedHashMap<>();
        collectTemplateVariableChanges(request.workbookSnapshot(), mock, changes);
        for (Map<String, Object> mapping : request.mappings() == null ? List.<Map<String, Object>>of() : request.mappings()) {
            String path = String.valueOf(mapping.getOrDefault("jsonPath", "")); JsonNode value = jsonPath(mock, path);
            if (value != null && !value.isMissingNode()) {
                String sheetId = String.valueOf(mapping.getOrDefault("sheetId", "sheet-1"));
                String cellAddress = String.valueOf(mapping.getOrDefault("cellAddress", ""));
                if (StringUtils.hasText(cellAddress)) putChange(changes, sheetId, cellAddress, jsonValue(value));
            }
        }
        return Map.of("workbookSnapshot", request.workbookSnapshot(), "changedCells", new ArrayList<>(changes.values()));
    }

    private void collectTemplateVariableChanges(Object workbookSnapshot, JsonNode mock, Map<String, Map<String, Object>> changes) {
        JsonNode sheets = objectMapper.valueToTree(workbookSnapshot).path("sheets");
        sheets.fields().forEachRemaining(sheetEntry -> {
            String sheetId = sheetEntry.getKey();
            sheetEntry.getValue().path("cellData").fields().forEachRemaining(rowEntry -> {
                int row = parseCellIndex(rowEntry.getKey());
                if (row < 0) return;
                rowEntry.getValue().fields().forEachRemaining(columnEntry -> {
                    int column = parseCellIndex(columnEntry.getKey());
                    JsonNode cellValue = columnEntry.getValue().path("v");
                    if (column < 0 || !cellValue.isTextual()) return;
                    String template = cellValue.asText();
                    Matcher matcher = TEMPLATE_VARIABLE.matcher(template);
                    StringBuffer rendered = new StringBuffer();
                    boolean resolved = false;
                    while (matcher.find()) {
                        JsonNode value = jsonPath(mock, "$." + matcher.group(1));
                        String replacement = matcher.group();
                        if (value != null && !value.isMissingNode() && !value.isNull()) {
                            replacement = jsonValue(value);
                            resolved = true;
                        }
                        matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
                    }
                    matcher.appendTail(rendered);
                    if (resolved && !template.contentEquals(rendered)) {
                        putChange(changes, sheetId, cellAddress(row, column), rendered.toString());
                    }
                });
            });
        });
    }

    private void putChange(Map<String, Map<String, Object>> changes, String sheetId, String cellAddress, String value) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("sheetId", sheetId);
        change.put("cellAddress", cellAddress);
        change.put("value", value);
        changes.put(sheetId + "\u0000" + cellAddress, change);
    }

    private String cellAddress(int row, int column) {
        StringBuilder name = new StringBuilder();
        int value = column + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.append(row + 1).toString();
    }

    private int parseCellIndex(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException exception) { return -1; }
    }

    private String jsonValue(JsonNode value) { return value.isValueNode() ? value.asText() : value.toString(); }

    @Transactional
    public Map<String, Object> createExport(Long templateId, WorkshopRequests.ExportCreateRequest request) {
        if (templateId != null) template(templateId);
        String taskId = "export_" + UUID.randomUUID().toString().replace("-", ""); String name = requireFileName(request.fileName()); Instant now = Instant.now();
        jdbcTemplate.update("INSERT INTO pyin_export_workshop_export_task (task_id, template_id, file_name, status, progress, preview_image, created_at, updated_at) VALUES (?, ?, ?, 'UPLOADING', 5, ?, ?, ?)", taskId, templateId, name, request.previewImage(), now, now);
        return task(taskId);
    }

    @Transactional
    public Map<String, Object> uploadExport(String taskId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("导出文件不能为空"); if (file.getSize() > properties.getMaxFileSizeBytes()) throw invalid("导出文件超过大小限制");
        task(taskId);
        try {
            Path output = storagePath("exports", taskId + ".xlsx"); Files.write(output, file.getBytes());
            jdbcTemplate.update("UPDATE pyin_export_workshop_export_task SET status = 'COMPLETED', progress = 100, output_path = ?, updated_at = ? WHERE task_id = ?", output.toString(), Instant.now(), taskId);
            return task(taskId);
        } catch (IOException exception) { throw invalid("保存导出文件失败"); }
    }

    public Map<String, Object> task(String taskId) {
        List<Map<String, Object>> results = jdbcTemplate.query("SELECT * FROM pyin_export_workshop_export_task WHERE task_id = ?", (rs, row) -> {
            Map<String, Object> task = new LinkedHashMap<>(); task.put("taskId", rs.getString("task_id")); task.put("templateId", rs.getObject("template_id"));
            task.put("fileName", rs.getString("file_name")); task.put("status", rs.getString("status")); task.put("progress", rs.getInt("progress"));
            task.put("previewImage", rs.getString("preview_image")); task.put("errorMessage", rs.getString("error_message"));
            task.put("downloadUrl", "COMPLETED".equals(rs.getString("status")) ? "/plugins/export-workshop/admin/exports/" + rs.getString("task_id") + "/download" : null); return task;
        }, taskId);
        if (results.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "导出任务不存在"); return results.get(0);
    }

    public Path exportFile(String taskId) {
        task(taskId); String output = jdbcTemplate.queryForObject("SELECT output_path FROM pyin_export_workshop_export_task WHERE task_id = ?", String.class, taskId);
        if (!StringUtils.hasText(output) || !Files.isRegularFile(Path.of(output))) throw new BusinessException(ErrorCode.NOT_FOUND, "导出文件尚未生成"); return Path.of(output);
    }

    private Map<String, Object> forkAndSave(Long id, WorkshopRequests.WorkbookSaveRequest request) {
        Map<String, Object> child = fork(id); Long childId = asLong(child.get("id")); return saveWorkbook(childId, request);
    }

    private Map<String, Object> createTemplate(Long directoryId, String name, String sourceType, String sourceUri, boolean readOnly, String snapshot, String mappings, String originalFilePath, String checksum) { return createTemplate(directoryId, name, sourceType, sourceUri, readOnly, snapshot, mappings, originalFilePath, checksum, null); }
    private Map<String, Object> createTemplate(Long directoryId, String name, String sourceType, String sourceUri, boolean readOnly, String snapshot, String mappings, String originalFilePath, String checksum, Long parentTemplateId) {
        validateParentDirectory(directoryId); Instant now = Instant.now();
        jdbcTemplate.update("INSERT INTO pyin_export_workshop_template (directory_id, name, source_type, source_uri, source_checksum, parent_template_id, read_only, workbook_snapshot, mappings_json, original_file_path, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", directoryId, name, sourceType, sourceUri, checksum, parentTemplateId, readOnly, snapshot, mappings, originalFilePath, now, now);
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM pyin_export_workshop_template", Long.class);
        jdbcTemplate.update("INSERT INTO pyin_export_workshop_version (template_id, version_no, workbook_snapshot, mappings_json, created_at) VALUES (?, 1, ?, ?, ?)", id, snapshot, mappings, now);
        return template(id);
    }

    private Map<String, Object> template(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query("SELECT * FROM pyin_export_workshop_template WHERE id = ? AND deleted = FALSE", (rs, row) -> {
            Map<String, Object> item = new LinkedHashMap<>(); item.put("id", rs.getLong("id")); item.put("directoryId", rs.getObject("directory_id")); item.put("name", rs.getString("name"));
            item.put("sourceType", rs.getString("source_type")); item.put("sourceUri", rs.getString("source_uri")); item.put("sourceChecksum", rs.getString("source_checksum")); item.put("readOnly", rs.getBoolean("read_only")); item.put("parentTemplateId", rs.getObject("parent_template_id"));
            item.put("workbookSnapshot", parse(rs.getString("workbook_snapshot"))); item.put("mappings", parse(rs.getString("mappings_json"))); return item;
        }, id);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"); return rows.get(0);
    }

    private Path resolveConfiguredRoot(String requested) {
        if (!StringUtils.hasText(requested)) throw invalid("请选择本地来源目录"); Path candidate = Path.of(requested).toAbsolutePath().normalize();
        boolean allowed = properties.getLocalRoots().stream().filter(StringUtils::hasText).map(root -> Path.of(root).toAbsolutePath().normalize()).anyMatch(candidate::equals);
        if (!allowed || !Files.isDirectory(candidate)) throw invalid("本地来源目录未配置或不可访问"); return candidate;
    }
    private URI validateRemoteUri(String value) {
        try { URI uri = URI.create(value); if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) throw invalid("仅允许 HTTP(S) 网络模板"); if (!StringUtils.hasText(uri.getHost())) throw invalid("网络模板地址无效");
            InetAddress address = InetAddress.getByName(uri.getHost()); if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress() || address.isMulticastAddress()) throw invalid("网络模板地址不允许访问内网"); return uri;
        } catch (IllegalArgumentException | IOException exception) { if (exception instanceof BusinessException businessException) throw businessException; throw invalid("网络模板地址无效"); }
    }
    private boolean isSupported(Path path) { return SUPPORTED_EXTENSIONS.contains(extension(path.getFileName().toString())); }
    private void validateExtension(String extension) { if (!SUPPORTED_EXTENSIONS.contains(extension)) throw invalid("仅支持 .xlsx 或 .json 模板"); }
    private void validateParentDirectory(Long id) { if (id == null) return; Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_export_workshop_directory WHERE id = ? AND deleted = FALSE", Integer.class, id); if (count == null || count == 0) throw new BusinessException(ErrorCode.NOT_FOUND, "目录不存在"); }
    private String requireName(String value) { if (!StringUtils.hasText(value) || value.trim().length() > 128 || value.contains("/") || value.contains("\\\\")) throw invalid("名称无效"); return value.trim(); }
    private String requireFileName(String value) { String name = requireName(StringUtils.hasText(value) ? value : "export.xlsx"); return name.endsWith(".xlsx") ? name : name + ".xlsx"; }
    private Long parseNodeId(String value) { try { return Long.parseLong(value.substring(value.indexOf('-') + 1)); } catch (RuntimeException exception) { throw invalid("资源节点标识无效"); } }
    private Path storagePath(String... parts) { Path path = Path.of(properties.getStorageRoot()); for (String part : parts) path = path.resolve(part); return path.toAbsolutePath().normalize(); }
    private String serialize(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw invalid("工作簿数据无法序列化"); } }
    private Object parse(String value) { try { return StringUtils.hasText(value) ? objectMapper.readValue(value, Object.class) : List.of(); } catch (JsonProcessingException exception) { throw invalid("工作簿数据已损坏"); } }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> castMappings(Object value) { return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of(); }
    private void validateWorkbookJson(String value) { try { objectMapper.readTree(value); } catch (JsonProcessingException exception) { throw invalid("工作簿快照必须是有效 JSON"); } }
    private String defaultWorkbook(String name) { return "{\"id\":\"workbook-" + UUID.randomUUID() + "\",\"name\":\"" + name.replace("\"", "") + "\",\"sheetOrder\":[\"sheet-1\"],\"sheets\":{\"sheet-1\":{\"id\":\"sheet-1\",\"name\":\"Sheet1\",\"cellData\":{}}},\"resources\":[]}"; }
    private String extension(String value) { int index = value == null ? -1 : value.lastIndexOf('.'); return index < 0 ? "" : value.substring(index + 1).toLowerCase(); }
    private String stripExtension(String value) { String safe = StringUtils.hasText(value) ? value : "未命名模板"; int index = safe.lastIndexOf('.'); return index > 0 ? safe.substring(0, index) : safe; }
    private String sha256(byte[] content) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    private JsonNode jsonPath(JsonNode root, String path) { if (!StringUtils.hasText(path) || !path.startsWith("$")) return null; JsonNode current = root; for (String segment : path.substring(1).split("\\.")) { if (segment.isBlank()) continue; current = current == null ? null : current.path(segment); } return current; }
    private long asLong(Object value) { return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value)); }
    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.INVALID_REQUEST, message); }
}
