package com.pyin.plugin.exportworkshop.web;

import java.util.List;
import java.util.Map;

public final class WorkshopRequests {

    private WorkshopRequests() { }

    public record FolderSaveRequest(Long parentId, String name) { }
    public record BlankTemplateRequest(Long directoryId, String id, String name) { }
    public record RenameRequest(String name) { }
    public record NetworkMountRequest(Long directoryId, String id, String name, String url) { }
    public record LocalDirectoryMountRequest(Long directoryId, String idPrefix, String root) { }
    public record WorkbookSaveRequest(String name, Object workbookSnapshot, List<Map<String, Object>> mappings) { }
    public record DebugRequest(Object workbookSnapshot, Object mockData, List<Map<String, Object>> mappings) { }
    public record ExportCreateRequest(String fileName, String previewImage) { }
}
