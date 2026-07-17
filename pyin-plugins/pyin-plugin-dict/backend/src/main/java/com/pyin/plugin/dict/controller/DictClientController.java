package com.pyin.plugin.dict.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.sdk.annotation.OpenMapping;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 字典客户端控制器，对外提供 C 端 API。
 */
@OpenMapping
public class DictClientController {

    @GetMapping("/label")
    public Result<Map<String, String>> label() {
        return Result.ok(Map.of("label", "演示标签"));
    }

    @GetMapping("/items")
    public Result<List<Map<String, String>>> items() {
        return Result.ok(List.of(Map.of("value", "1", "label", "演示项")));
    }

    @GetMapping("/batch")
    public Result<Map<String, Object>> batch() {
        return Result.ok(Map.of("gender", List.of(Map.of("value", "1", "label", "男"))));
    }
}
