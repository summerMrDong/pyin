package com.pyin.plugin.statemachine.web;

import java.util.List;

public final class StateMachineRequests {

    private StateMachineRequests() {
    }

    public record DefinitionSaveRequest(String machineKey, String machineName, List<StateNode> nodes,
                                        List<Transition> transitions) {
    }

    public record StateNode(String id, String name, String type, String description, Integer x, Integer y) {
    }

    public record Transition(String id, String source, String target, String eventCode, String eventName,
                             String condition, String actions) {
    }

    public record DebugEventRequest(String eventCode) {
    }
}
