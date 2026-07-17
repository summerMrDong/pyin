package com.pyin.plugin.config.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import com.pyin.plugin.config.service.ConfigAdminService;
import com.pyin.plugin.config.web.ConfigItemSaveRequest;
import com.pyin.plugin.config.web.ConfigNamespaceSaveRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 配置管理后台控制器。
 */
@AdminMapping
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
        try {
            return Result.ok(configAdminService.saveNamespace(request));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "config:delete", name = "配置删除")
    @DeleteMapping("/namespaces/{id}")
    public Result<?> deleteNamespace(@PathVariable Long id) {
        try {
            configAdminService.deleteNamespace(id);
            return Result.ok();
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "config:view", name = "配置查看")
    @GetMapping("/items")
    public Result<List<Map<String, Object>>> listItems(
            @RequestParam(required = false) Long namespaceId,
            @RequestParam(required = false) String keyword
    ) {
        try {
            return Result.ok(configAdminService.listItems(namespaceId, keyword));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "config:view", name = "配置查看")
    @GetMapping("/items/{id}")
    public Result<?> getItem(@PathVariable Long id) {
        try {
            return Result.ok(configAdminService.getItem(id));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "config:update", name = "配置修改")
    @PostMapping("/items")
    public Result<?> saveItem(@RequestBody ConfigItemSaveRequest request) {
        try {
            return Result.ok(configAdminService.saveItem(request));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "config:delete", name = "配置删除")
    @DeleteMapping("/items/{id}")
    public Result<?> deleteItem(@PathVariable Long id) {
        try {
            configAdminService.deleteItem(id);
            return Result.ok();
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }
}
