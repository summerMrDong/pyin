package com.pyin.plugin.system.setting.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;

@AdminMapping("/system-settings")
public class SystemSettingController {

    @Permission(code = "system:view", name = "系统查看")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(Map.of("settingKey", "system.name", "settingValue", "Pyin")));
    }
}
