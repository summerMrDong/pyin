package com.pyin.plugin.config.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.config.service.ConfigAdminService;
import com.pyin.plugin.config.web.ConfigDirectoryMoveRequest;
import com.pyin.plugin.config.web.ConfigDirectorySaveRequest;
import com.pyin.plugin.config.web.ConfigDirectoryTreeNode;
import com.pyin.plugin.config.web.ConfigItemSaveRequest;
import com.pyin.plugin.config.web.ConfigNamespaceSaveRequest;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 配置管理后台控制器。
 */
@AdminMapping("/config")
public class ConfigAdminController {

    private final ConfigAdminService configAdminService;

    public ConfigAdminController(ConfigAdminService configAdminService) {
        this.configAdminService = configAdminService;
    }

    @Permission(code = "config:view", name = "配置查看")
    @GetMapping("/namespaces")
    public Result<List<Map<String, Object>>> listNamespaces() {
        return Result.ok(configAdminService.listNamespaces());
    }

    @Permission(code = "config:update", name = "配置修改")
    @PostMapping("/namespaces")
    public Result<?> saveNamespace(@RequestBody ConfigNamespaceSaveRequest request) {
        return execute(() -> configAdminService.saveNamespace(request));
    }

    @Permission(code = "config:delete", name = "配置删除")
    @DeleteMapping("/namespaces/{id}")
    public Result<?> deleteNamespace(@PathVariable Long id) {
        return execute(() -> {
            configAdminService.deleteNamespace(id);
            return null;
        });
    }

    @Permission(code = "config:view", name = "配置查看")
    @GetMapping("/namespaces/{namespaceId}/directories/tree")
    public Result<List<ConfigDirectoryTreeNode>> listDirectoryTree(@PathVariable Long namespaceId) {
        try {
            return Result.ok(configAdminService.listDirectoryTree(namespaceId));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "config:update", name = "配置修改")
    @PostMapping("/directories")
    public Result<?> saveDirectory(@RequestBody ConfigDirectorySaveRequest request) {
        return execute(() -> configAdminService.saveDirectory(request));
    }

    @Permission(code = "config:update", name = "配置修改")
    @PutMapping("/directories/{id}")
    public Result<?> updateDirectory(@PathVariable Long id, @RequestBody ConfigDirectorySaveRequest request) {
        request.setId(id);
        return execute(() -> configAdminService.saveDirectory(request));
    }

    @Permission(code = "config:update", name = "配置修改")
    @PutMapping("/directories/{id}/move")
    public Result<?> moveDirectory(@PathVariable Long id, @RequestBody ConfigDirectoryMoveRequest request) {
        return execute(() -> {
            configAdminService.moveDirectory(id, request);
            return null;
        });
    }

    @Permission(code = "config:delete", name = "配置删除")
    @DeleteMapping("/directories/{id}")
    public Result<?> deleteDirectory(@PathVariable Long id) {
        return execute(() -> {
            configAdminService.deleteDirectory(id);
            return null;
        });
    }

    @Permission(code = "config:view", name = "配置查看")
    @GetMapping("/items")
    public Result<List<Map<String, Object>>> listItems(
            @RequestParam(required = false) Long namespaceId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long directoryId
    ) {
        try {
            return Result.ok(configAdminService.listItems(namespaceId, keyword, directoryId));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "config:view", name = "配置查看")
    @GetMapping("/items/{id}")
    public Result<?> getItem(@PathVariable Long id) {
        return execute(() -> configAdminService.getItem(id));
    }

    @Permission(code = "config:update", name = "配置修改")
    @PostMapping("/items")
    public Result<?> saveItem(@RequestBody ConfigItemSaveRequest request) {
        return execute(() -> configAdminService.saveItem(request));
    }

    @Permission(code = "config:delete", name = "配置删除")
    @DeleteMapping("/items/{id}")
    public Result<?> deleteItem(@PathVariable Long id) {
        return execute(() -> {
            configAdminService.deleteItem(id);
            return null;
        });
    }

    private Result<?> execute(ThrowingSupplier<Object> action) {
        try {
            Object result = action.get();
            return result == null ? Result.ok() : Result.ok(result);
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
