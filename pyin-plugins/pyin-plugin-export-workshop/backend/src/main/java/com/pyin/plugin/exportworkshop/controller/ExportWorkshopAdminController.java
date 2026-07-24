package com.pyin.plugin.exportworkshop.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.exportworkshop.service.ExportWorkshopService;
import com.pyin.plugin.exportworkshop.web.WorkshopRequests;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@AdminMapping
public class ExportWorkshopAdminController {

    private final ExportWorkshopService service;

    public ExportWorkshopAdminController(ExportWorkshopService service) { this.service = service; }

    @Permission(code = "export-workshop:view", name = "查看导出工坊")
    @GetMapping("/templates/tree")
    public Result<List<Map<String, Object>>> tree() { return Result.ok(service.listTree()); }

    @Permission(code = "export-workshop:view", name = "查看导出工坊")
    @GetMapping("/sources/local-roots")
    public Result<List<String>> localRoots() { return Result.ok(service.localRoots()); }

    @Permission(code = "export-workshop:create", name = "创建导出模板")
    @PostMapping("/folders")
    public Result<?> createFolder(@RequestBody WorkshopRequests.FolderSaveRequest request) { return execute(() -> service.createFolder(request)); }

    @Permission(code = "export-workshop:create", name = "创建导出模板")
    @PostMapping("/templates/blank")
    public Result<?> createBlank(@RequestBody WorkshopRequests.BlankTemplateRequest request) { return execute(() -> service.createBlank(request)); }

    @Permission(code = "export-workshop:import", name = "导入导出模板")
    @PostMapping("/templates/import")
    public Result<?> importTemplate(@RequestParam(required = false) Long directoryId, @RequestPart("file") MultipartFile file) { return execute(() -> service.importFile(directoryId, file)); }

    @Permission(code = "export-workshop:mount", name = "挂载导出模板")
    @PostMapping("/templates/mount/local-directory")
    public Result<?> mountLocalDirectory(@RequestBody WorkshopRequests.LocalDirectoryMountRequest request) { return execute(() -> service.mountLocalDirectory(request)); }

    @Permission(code = "export-workshop:mount", name = "挂载导出模板")
    @PostMapping("/templates/mount/network")
    public Result<?> mountNetwork(@RequestBody WorkshopRequests.NetworkMountRequest request) { return execute(() -> service.mountNetwork(request)); }

    @Permission(code = "export-workshop:view", name = "查看导出工坊")
    @GetMapping("/templates/{id}")
    public Result<?> template(@PathVariable Long id) { return execute(() -> service.readTemplate(id)); }

    @Permission(code = "export-workshop:update", name = "编辑导出模板")
    @PutMapping("/templates/{id}/workbook")
    public Result<?> saveWorkbook(@PathVariable Long id, @RequestBody WorkshopRequests.WorkbookSaveRequest request) { return execute(() -> service.saveWorkbook(id, request)); }

    @Permission(code = "export-workshop:update", name = "编辑导出模板")
    @PutMapping("/nodes/{nodeId}/name")
    public Result<?> rename(@PathVariable String nodeId, @RequestBody WorkshopRequests.RenameRequest request) { return execute(() -> service.rename(nodeId, request)); }

    @Permission(code = "export-workshop:delete", name = "删除导出模板")
    @DeleteMapping("/nodes/{nodeId}")
    public Result<?> delete(@PathVariable String nodeId) { return execute(() -> { service.delete(nodeId); return null; }); }

    @Permission(code = "export-workshop:update", name = "编辑导出模板")
    @PostMapping("/templates/{id}/fork")
    public Result<?> fork(@PathVariable Long id) { return execute(() -> service.fork(id)); }

    @Permission(code = "export-workshop:debug", name = "调试导出模板")
    @PostMapping("/debug/render")
    public Result<?> debug(@RequestBody WorkshopRequests.DebugRequest request) { return execute(() -> service.debug(request)); }

    @Permission(code = "export-workshop:export", name = "导出模板文件")
    @PostMapping("/templates/{templateId}/exports")
    public Result<?> createExport(@PathVariable Long templateId, @RequestBody WorkshopRequests.ExportCreateRequest request) { return execute(() -> service.createExport(templateId, request)); }

    @Permission(code = "export-workshop:export", name = "导出模板文件")
    @PostMapping("/exports/{taskId}/file")
    public Result<?> uploadExport(@PathVariable String taskId, @RequestPart("file") MultipartFile file) { return execute(() -> service.uploadExport(taskId, file)); }

    @Permission(code = "export-workshop:export", name = "导出模板文件")
    @GetMapping("/exports/{taskId}")
    public Result<?> task(@PathVariable String taskId) { return execute(() -> service.task(taskId)); }

    @Permission(code = "export-workshop:export", name = "导出模板文件")
    @GetMapping("/exports/{taskId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String taskId) throws IOException {
        Path file = service.exportFile(taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(file.getFileName().toString(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(Files.size(file))
                .body(new InputStreamResource(Files.newInputStream(file)));
    }

    private Result<?> execute(ThrowingSupplier action) {
        try { Object value = action.get(); return value == null ? Result.ok() : Result.ok(value); }
        catch (BusinessException exception) { return Result.fail(exception.getCode(), exception.getMessage()); }
    }

    @FunctionalInterface
    private interface ThrowingSupplier { Object get(); }
}
