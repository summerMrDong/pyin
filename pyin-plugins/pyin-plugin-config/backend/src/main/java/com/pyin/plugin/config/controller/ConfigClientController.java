package com.pyin.plugin.config.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.sdk.annotation.OpenMapping;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 配置客户端控制器，对外提供 C 端 API。
 */
@OpenMapping
public class ConfigClientController {

    @GetMapping("/value")
    public Result<Map<String, Object>> value() {
        return Result.ok(Map.of("key", "demo.key", "value", "demo"));
    }

    @GetMapping("/namespace")
    public Result<Map<String, String>> namespace() {
        return Result.ok(Map.of("demo.key", "demo"));
    }

    @GetMapping("/version")
    public Result<Map<String, Object>> version() {
        return Result.ok(Map.of("version", 1L));
    }
}
