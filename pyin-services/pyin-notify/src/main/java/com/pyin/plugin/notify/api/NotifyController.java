package com.pyin.plugin.notify.api;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.notify.model.NotifyEvent;
import com.pyin.plugin.notify.service.NotifyService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class NotifyController {

    private final NotifyService notifyService;

    public NotifyController(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @GetMapping("/open/events/stream")
    public SseEmitter stream() {
        return notifyService.connect();
    }

    @PostMapping("/api/core/notify/publish")
    public Result<Map<String, Object>> publish(@RequestBody NotifyEvent event) {
        notifyService.publish(event);
        return Result.ok(Map.of("published", true, "eventType", event.eventType()));
    }
}
