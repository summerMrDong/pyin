package com.pyin.plugin.statemachine.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import com.pyin.plugin.statemachine.service.StateMachineDesignerService;
import com.pyin.plugin.statemachine.web.StateMachineRequests.DebugEventRequest;
import com.pyin.plugin.statemachine.web.StateMachineRequests.DefinitionSaveRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@AdminMapping("/state-machines")
public class StateMachineAdminController {

    private final StateMachineDesignerService designerService;

    public StateMachineAdminController(StateMachineDesignerService designerService) {
        this.designerService = designerService;
    }

    @Permission(code = "state-machine:view", name = "状态机查看")
    @GetMapping("/order")
    public Result<Map<String, Object>> getOrderMachine() {
        return Result.ok(designerService.workspace());
    }

    @Permission(code = "state-machine:edit", name = "状态机编辑")
    @PostMapping("/order")
    public Result<?> saveOrderMachine(@RequestBody DefinitionSaveRequest request) {
        return execute(() -> designerService.save(request));
    }

    @Permission(code = "state-machine:publish", name = "状态机发布")
    @PostMapping("/order/publish")
    public Result<?> publishOrderMachine() {
        return execute(designerService::publish);
    }

    @Permission(code = "state-machine:debug", name = "状态机调试")
    @PostMapping("/order/debug/reset")
    public Result<?> resetDebugSession() {
        return execute(designerService::resetDebugSession);
    }

    @Permission(code = "state-machine:debug", name = "状态机调试")
    @PostMapping("/order/debug/events")
    public Result<?> dispatchDebugEvent(@RequestBody DebugEventRequest request) {
        return execute(() -> designerService.dispatchDebugEvent(request));
    }

    private Result<?> execute(java.util.concurrent.Callable<Map<String, Object>> action) {
        try {
            return Result.ok(action.call());
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            return Result.fail("STATE_MACHINE_ERROR", exception.getMessage());
        }
    }
}
