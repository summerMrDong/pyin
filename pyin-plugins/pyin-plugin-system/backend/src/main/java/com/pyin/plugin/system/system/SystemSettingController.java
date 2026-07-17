package com.pyin.plugin.system.system;

import com.pyin.plugin.common.api.Result;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/system-settings")
public class SystemSettingController {

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(Map.of("settingKey", "system.name", "settingValue", "Pyin")));
    }
}
