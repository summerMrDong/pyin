package com.pyin.plugin.exportworkshop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.exportworkshop.support.ExportWorkshopProperties;
import com.pyin.plugin.exportworkshop.web.WorkshopRequests;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

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
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, template_code VARCHAR(80), directory_id BIGINT, name VARCHAR(255) NOT NULL,
                    source_type VARCHAR(32) NOT NULL, source_uri VARCHAR(2048), source_checksum VARCHAR(128),
                    source_etag VARCHAR(256), source_updated_at TIMESTAMP, parent_template_id BIGINT,
                    read_only BOOLEAN NOT NULL DEFAULT FALSE, workbook_snapshot CLOB NOT NULL, mappings_json CLOB,
                    original_file_path VARCHAR(1024), deleted BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)
                """);
        jdbcTemplate.execute("ALTER TABLE pyin_export_workshop_template ADD COLUMN IF NOT EXISTS template_code VARCHAR(80)");
        jdbcTemplate.update("UPDATE pyin_export_workshop_template SET template_code = CONCAT('tpl_', id) WHERE template_code IS NULL OR template_code = ''");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_pyin_export_workshop_template_code ON pyin_export_workshop_template(template_code)");
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
        jdbcTemplate.query("SELECT id, template_code, directory_id, name, source_type, read_only, parent_template_id, updated_at FROM pyin_export_workshop_template WHERE deleted = FALSE ORDER BY name", rs -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", "template-" + rs.getLong("id")); node.put("nodeType", "TEMPLATE");
            node.put("parentId", rs.getObject("directory_id") == null ? null : "directory-" + rs.getLong("directory_id"));
            node.put("name", rs.getString("name")); node.put("templateId", rs.getString("template_code")); node.put("sourceType", rs.getString("source_type"));
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
        String name = requireName(request.name());
        return createTemplate(request.directoryId(), name, "ONLINE", null, false, defaultWorkbook(name), "[]", null, null, null, request.id());
    }

    @Transactional
    public Map<String, Object> importFile(Long directoryId, String templateId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("上传文件不能为空");
        if (file.getSize() > properties.getMaxFileSizeBytes()) throw invalid("模板文件超过大小限制");
        String name = requireName(stripExtension(file.getOriginalFilename())); String extension = extension(file.getOriginalFilename());
        validateExtension(extension);
        try {
            byte[] content = file.getBytes();
            String snapshot = snapshotFromSource(name, extension, content);
            validateWorkbookJson(snapshot);
            Path target = storagePath("sources", UUID.randomUUID() + "." + extension);
            Files.write(target, content);
            return createTemplate(directoryId, name, "UPLOAD", file.getOriginalFilename(), false, snapshot, "[]", target.toString(), sha256(content), null, templateId);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "读取或解析上传模板失败");
        }
    }

    @Transactional
    public List<Map<String, Object>> mountLocalDirectory(WorkshopRequests.LocalDirectoryMountRequest request) {
        Path root = resolveConfiguredRoot(request.root()); validateParentDirectory(request.directoryId());
        try (var paths = Files.walk(root)) {
            List<Map<String, Object>> mounted = new ArrayList<>();
            List<Path> files = paths.filter(Files::isRegularFile).filter(this::isSupported).sorted().toList();
            int sequence = 0;
            for (Path path : files) {
                try {
                    byte[] content = Files.readAllBytes(path);
                    if (content.length > properties.getMaxFileSizeBytes()) continue;
                    String extension = extension(path.getFileName().toString());
                    String name = stripExtension(path.getFileName().toString());
                    String snapshot = snapshotFromSource(name, extension, content);
                    validateWorkbookJson(snapshot);
                    String templateId = StringUtils.hasText(request.idPrefix()) ? request.idPrefix().trim() + "_" + (++sequence) : null;
                    mounted.add(createTemplate(request.directoryId(), name, "LOCAL_DIRECTORY", path.toString(), true, snapshot, "[]", null, sha256(content), null, templateId));
                } catch (IOException ignored) { }
            }
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
            String snapshot = snapshotFromSource(name, extension, content);
            validateWorkbookJson(snapshot);
            return createTemplate(request.directoryId(), name, "NETWORK", uri.toString(), true, snapshot, "[]", null, sha256(content), null, request.id());
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

    /** 用于 HTTP 下载响应的模板文件。 */
    public record DownloadedTemplate(String fileName, byte[] content) { }

    public DownloadedTemplate downloadTemplate(Long id) { return writeTemplateXlsx(template(id)); }

    public DownloadedTemplate downloadTemplateById(String templateId) {
        List<Long> ids = jdbcTemplate.query("SELECT id FROM pyin_export_workshop_template WHERE template_code = ? AND deleted = FALSE", (rs, row) -> rs.getLong("id"), templateId);
        if (ids.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在");
        return downloadTemplate(ids.get(0));
    }

    private DownloadedTemplate writeTemplateXlsx(Map<String, Object> template) {
        JsonNode snapshot = objectMapper.valueToTree(template.get("workbookSnapshot"));
        JsonNode sheets = snapshot.path("sheets");
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            List<String> sheetIds = new ArrayList<>();
            snapshot.path("sheetOrder").forEach(item -> sheetIds.add(item.asText()));
            if (sheetIds.isEmpty()) sheets.fieldNames().forEachRemaining(sheetIds::add);
            for (String sheetId : sheetIds) {
                JsonNode source = sheets.path(sheetId);
                if (source.isMissingNode()) continue;
                Sheet target = workbook.createSheet(WorkbookUtil.createSafeSheetName(source.path("name").asText(sheetId)));
                source.path("rowData").fields().forEachRemaining(entry -> {
                    int rowIndex = parseCellIndex(entry.getKey());
                    if (rowIndex < 0) return;
                    Row row = target.createRow(rowIndex);
                    if (entry.getValue().path("h").isNumber()) row.setHeightInPoints((float) (entry.getValue().path("h").asDouble() * 72 / 96));
                });
                source.path("cellData").fields().forEachRemaining(rowEntry -> {
                    int rowIndex = parseCellIndex(rowEntry.getKey());
                    if (rowIndex < 0) return;
                    Row existingRow = target.getRow(rowIndex);
                    Row row = existingRow == null ? target.createRow(rowIndex) : existingRow;
                    rowEntry.getValue().fields().forEachRemaining(cellEntry -> {
                        int columnIndex = parseCellIndex(cellEntry.getKey());
                        if (columnIndex < 0) return;
                        writeWorkbookCell(row.createCell(columnIndex), cellEntry.getValue());
                    });
                });
                source.path("columnData").fields().forEachRemaining(entry -> {
                    int columnIndex = parseCellIndex(entry.getKey());
                    if (columnIndex < 0 || !entry.getValue().path("w").isNumber()) return;
                    int width = (int) Math.round((entry.getValue().path("w").asDouble() - 5) / 7 * 256);
                    target.setColumnWidth(columnIndex, Math.max(0, Math.min(width, 255 * 256)));
                });
                for (JsonNode merge : source.path("mergeData")) {
                    if (merge.has("startRow") && merge.has("startColumn") && merge.has("endRow") && merge.has("endColumn")) {
                        target.addMergedRegion(new CellRangeAddress(merge.path("startRow").asInt(), merge.path("endRow").asInt(), merge.path("startColumn").asInt(), merge.path("endColumn").asInt()));
                    }
                }
            }
            if (workbook.getNumberOfSheets() == 0) workbook.createSheet("Sheet1");
            workbook.setForceFormulaRecalculation(true); workbook.write(output);
            return new DownloadedTemplate(requireFileName(String.valueOf(template.get("name"))), output.toByteArray());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "生成模板下载文件失败");
        }
    }

    private void writeWorkbookCell(Cell target, JsonNode source) {
        JsonNode formula = source.path("f");
        if (formula.isTextual() && StringUtils.hasText(formula.asText())) {
            String value = formula.asText(); target.setCellFormula(value.startsWith("=") ? value.substring(1) : value); return;
        }
        JsonNode value = source.path("v");
        if (value.isNumber()) target.setCellValue(value.asDouble());
        else if (value.isBoolean()) target.setCellValue(value.asBoolean());
        else if (!value.isMissingNode() && !value.isNull()) target.setCellValue(value.asText());
    }

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

    private Map<String, Object> createTemplate(Long directoryId, String name, String sourceType, String sourceUri, boolean readOnly, String snapshot, String mappings, String originalFilePath, String checksum) { return createTemplate(directoryId, name, sourceType, sourceUri, readOnly, snapshot, mappings, originalFilePath, checksum, null, null); }
    private Map<String, Object> createTemplate(Long directoryId, String name, String sourceType, String sourceUri, boolean readOnly, String snapshot, String mappings, String originalFilePath, String checksum, Long parentTemplateId) { return createTemplate(directoryId, name, sourceType, sourceUri, readOnly, snapshot, mappings, originalFilePath, checksum, parentTemplateId, null); }
    private Map<String, Object> createTemplate(Long directoryId, String name, String sourceType, String sourceUri, boolean readOnly, String snapshot, String mappings, String originalFilePath, String checksum, Long parentTemplateId, String requestedTemplateId) {
        validateParentDirectory(directoryId); Instant now = Instant.now();
        String templateId = normalizeTemplateId(requestedTemplateId);
        jdbcTemplate.update("INSERT INTO pyin_export_workshop_template (template_code, directory_id, name, source_type, source_uri, source_checksum, parent_template_id, read_only, workbook_snapshot, mappings_json, original_file_path, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", templateId, directoryId, name, sourceType, sourceUri, checksum, parentTemplateId, readOnly, snapshot, mappings, originalFilePath, now, now);
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM pyin_export_workshop_template", Long.class);
        jdbcTemplate.update("INSERT INTO pyin_export_workshop_version (template_id, version_no, workbook_snapshot, mappings_json, created_at) VALUES (?, 1, ?, ?, ?)", id, snapshot, mappings, now);
        return template(id);
    }

    private Map<String, Object> template(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query("SELECT * FROM pyin_export_workshop_template WHERE id = ? AND deleted = FALSE", (rs, row) -> {
            Map<String, Object> item = new LinkedHashMap<>(); item.put("id", rs.getLong("id")); item.put("templateId", rs.getString("template_code")); item.put("directoryId", rs.getObject("directory_id")); item.put("name", rs.getString("name"));
            item.put("sourceType", rs.getString("source_type")); item.put("sourceUri", rs.getString("source_uri")); item.put("sourceChecksum", rs.getString("source_checksum")); item.put("readOnly", rs.getBoolean("read_only")); item.put("parentTemplateId", rs.getObject("parent_template_id"));
            item.put("downloadUrl", "/plugins/export-workshop/admin/templates/download/" + rs.getString("template_code"));
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
    private String normalizeTemplateId(String value) {
        String templateId = StringUtils.hasText(value) ? value.trim() : "tpl_" + UUID.randomUUID().toString().replace("-", "");
        if (!templateId.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,79}")) throw invalid("模板 ID 仅支持字母、数字、下划线和短横线，且必须以字母或数字开头");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_export_workshop_template WHERE template_code = ?", Integer.class, templateId);
        if (count != null && count > 0) throw invalid("模板 ID 已存在");
        return templateId;
    }
    private String requireFileName(String value) { String name = requireName(StringUtils.hasText(value) ? value : "export.xlsx"); return name.endsWith(".xlsx") ? name : name + ".xlsx"; }
    private Long parseNodeId(String value) { try { return Long.parseLong(value.substring(value.indexOf('-') + 1)); } catch (RuntimeException exception) { throw invalid("资源节点标识无效"); } }
    private Path storagePath(String... parts) { Path path = Path.of(properties.getStorageRoot()); for (String part : parts) path = path.resolve(part); return path.toAbsolutePath().normalize(); }
    private String serialize(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw invalid("工作簿数据无法序列化"); } }
    private Object parse(String value) { try { return StringUtils.hasText(value) ? objectMapper.readValue(value, Object.class) : List.of(); } catch (JsonProcessingException exception) { throw invalid("工作簿数据已损坏"); } }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> castMappings(Object value) { return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of(); }
    private void validateWorkbookJson(String value) { try { objectMapper.readTree(value); } catch (JsonProcessingException exception) { throw invalid("工作簿快照必须是有效 JSON"); } }
    private String snapshotFromSource(String name, String extension, byte[] content) throws IOException {
        if ("json".equals(extension)) return new String(content, java.nio.charset.StandardCharsets.UTF_8);
        return serialize(readXlsxWorkbook(name, content));
    }

    /** 将 XLSX 中的可编辑内容转换为 Univer Sheets 的持久化快照。 */
    private Map<String, Object> readXlsxWorkbook(String name, byte[] content) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            Map<String, Object> sheets = new LinkedHashMap<>();
            List<String> sheetOrder = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                Sheet source = workbook.getSheetAt(index);
                String sheetId = "sheet-" + (index + 1);
                Map<String, Object> sheet = new LinkedHashMap<>();
                Map<String, Object> cellData = new LinkedHashMap<>();
                Map<String, Object> rowData = new LinkedHashMap<>();
                int maxColumn = -1;
                for (Row row : source) {
                    Map<String, Object> cells = new LinkedHashMap<>();
                    for (Cell cell : row) {
                        Map<String, Object> value = readCell(cell, formatter);
                        if (value.isEmpty()) continue;
                        cells.put(String.valueOf(cell.getColumnIndex()), value);
                        maxColumn = Math.max(maxColumn, cell.getColumnIndex());
                    }
                    if (!cells.isEmpty()) cellData.put(String.valueOf(row.getRowNum()), cells);
                    if (row.getHeight() != source.getDefaultRowHeight()) {
                        rowData.put(String.valueOf(row.getRowNum()), Map.of("h", Math.round(row.getHeightInPoints() * 96 / 72)));
                    }
                }
                List<Map<String, Integer>> mergeData = new ArrayList<>();
                for (int mergeIndex = 0; mergeIndex < source.getNumMergedRegions(); mergeIndex++) {
                    CellRangeAddress range = source.getMergedRegion(mergeIndex);
                    mergeData.add(Map.of("startRow", range.getFirstRow(), "startColumn", range.getFirstColumn(), "endRow", range.getLastRow(), "endColumn", range.getLastColumn()));
                    maxColumn = Math.max(maxColumn, range.getLastColumn());
                }
                Map<String, Object> columnData = new LinkedHashMap<>();
                int defaultWidth = source.getDefaultColumnWidth() * 256;
                for (int column = 0; column <= maxColumn; column++) {
                    int width = source.getColumnWidth(column);
                    if (width != defaultWidth) columnData.put(String.valueOf(column), Map.of("w", Math.round(width / 256.0 * 7 + 5)));
                }
                sheet.put("id", sheetId); sheet.put("name", source.getSheetName()); sheet.put("cellData", cellData);
                if (!rowData.isEmpty()) sheet.put("rowData", rowData);
                if (!columnData.isEmpty()) sheet.put("columnData", columnData);
                if (!mergeData.isEmpty()) sheet.put("mergeData", mergeData);
                sheets.put(sheetId, sheet); sheetOrder.add(sheetId);
            }
            if (sheets.isEmpty()) {
                Map<String, Object> emptySheet = new LinkedHashMap<>();
                emptySheet.put("id", "sheet-1"); emptySheet.put("name", "Sheet1"); emptySheet.put("cellData", Map.of());
                sheets.put("sheet-1", emptySheet); sheetOrder.add("sheet-1");
            }
            snapshot.put("id", "workbook-" + UUID.randomUUID()); snapshot.put("name", name);
            snapshot.put("sheetOrder", sheetOrder); snapshot.put("sheets", sheets); snapshot.put("resources", List.of());
            return snapshot;
        } catch (RuntimeException exception) {
            throw new IOException("XLSX 文件格式无效或已加密", exception);
        }
    }

    private Map<String, Object> readCell(Cell cell, DataFormatter formatter) {
        Map<String, Object> value = new LinkedHashMap<>();
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            value.put("f", "=" + cell.getCellFormula());
            type = cell.getCachedFormulaResultType();
        }
        switch (type) {
            case STRING -> value.put("v", cell.getStringCellValue());
            case NUMERIC -> value.put("v", DateUtil.isCellDateFormatted(cell) ? formatter.formatCellValue(cell) : cell.getNumericCellValue());
            case BOOLEAN -> value.put("v", cell.getBooleanCellValue());
            case ERROR -> value.put("v", formatter.formatCellValue(cell));
            default -> { }
        }
        return value;
    }
    private String defaultWorkbook(String name) { return "{\"id\":\"workbook-" + UUID.randomUUID() + "\",\"name\":\"" + name.replace("\"", "") + "\",\"sheetOrder\":[\"sheet-1\"],\"sheets\":{\"sheet-1\":{\"id\":\"sheet-1\",\"name\":\"Sheet1\",\"cellData\":{}}},\"resources\":[]}"; }
    private String extension(String value) { int index = value == null ? -1 : value.lastIndexOf('.'); return index < 0 ? "" : value.substring(index + 1).toLowerCase(); }
    private String stripExtension(String value) { String safe = StringUtils.hasText(value) ? value : "未命名模板"; int index = safe.lastIndexOf('.'); return index > 0 ? safe.substring(0, index) : safe; }
    private String sha256(byte[] content) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    private JsonNode jsonPath(JsonNode root, String path) { if (!StringUtils.hasText(path) || !path.startsWith("$")) return null; JsonNode current = root; for (String segment : path.substring(1).split("\\.")) { if (segment.isBlank()) continue; current = current == null ? null : current.path(segment); } return current; }
    private long asLong(Object value) { return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value)); }
    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.INVALID_REQUEST, message); }
}
