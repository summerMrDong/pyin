package com.pyin.plugin.dict.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import com.pyin.plugin.dict.service.DictAdminService;
import com.pyin.plugin.dict.web.DictItemSaveRequest;
import com.pyin.plugin.dict.web.DictTypeSaveRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 字典管理后台控制器。
 */
@AdminMapping
public class DictAdminController {

    private final DictAdminService dictAdminService;

    public DictAdminController(DictAdminService dictAdminService) {
        this.dictAdminService = dictAdminService;
    }

    @Permission(code = "dict:view", name = "字典查看")
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> listTypes() {
        return Result.ok(dictAdminService.listTypes());
    }

    @Permission(code = "dict:update", name = "字典修改")
    @PostMapping("/types")
    public Result<?> saveType(@RequestBody DictTypeSaveRequest request) {
        try {
            return Result.ok(dictAdminService.saveType(request));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "dict:delete", name = "字典删除")
    @DeleteMapping("/types/{id}")
    public Result<?> deleteType(@PathVariable Long id) {
        try {
            dictAdminService.deleteType(id);
            return Result.ok();
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "dict:view", name = "字典查看")
    @GetMapping("/items")
    public Result<List<Map<String, Object>>> listItems(@RequestParam(required = false) Long typeId) {
        try {
            return Result.ok(dictAdminService.listItems(typeId));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "dict:view", name = "字典查看")
    @GetMapping("/items/{id}")
    public Result<?> getItem(@PathVariable Long id) {
        try {
            return Result.ok(dictAdminService.getItem(id));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "dict:update", name = "字典修改")
    @PostMapping("/items")
    public Result<?> saveItem(@RequestBody DictItemSaveRequest request) {
        try {
            return Result.ok(dictAdminService.saveItem(request));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @Permission(code = "dict:delete", name = "字典删除")
    @DeleteMapping("/items/{id}")
    public Result<?> deleteItem(@PathVariable Long id) {
        try {
            dictAdminService.deleteItem(id);
            return Result.ok();
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }
}
