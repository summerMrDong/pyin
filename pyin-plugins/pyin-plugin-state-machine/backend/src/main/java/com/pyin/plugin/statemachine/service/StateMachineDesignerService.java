package com.pyin.plugin.statemachine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.statemachine.web.StateMachineRequests.DebugEventRequest;
import com.pyin.plugin.statemachine.web.StateMachineRequests.DefinitionSaveRequest;
import com.pyin.plugin.statemachine.web.StateMachineRequests.StateNode;
import com.pyin.plugin.statemachine.web.StateMachineRequests.Transition;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 状态机设计器的最小运行时：保存图定义、发布快照，并为在线调试维护独立会话。
 */
@Service
public class StateMachineDesignerService {

    private static final String ORDER_MACHINE_KEY = "order";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public StateMachineDesignerService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_state_machine_definition (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    machine_key VARCHAR(128) NOT NULL,
                    machine_name VARCHAR(128) NOT NULL,
                    definition_json CLOB NOT NULL,
                    published_version INT NOT NULL DEFAULT 0,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_state_machine_definition UNIQUE (machine_key)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_state_machine_version (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    machine_key VARCHAR(128) NOT NULL,
                    version_no INT NOT NULL,
                    definition_json CLOB NOT NULL,
                    published_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_state_machine_version UNIQUE (machine_key, version_no)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_state_machine_debug_session (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    machine_key VARCHAR(128) NOT NULL,
                    current_state_id VARCHAR(128) NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_state_machine_debug UNIQUE (machine_key)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_state_machine_debug_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    machine_key VARCHAR(128) NOT NULL,
                    event_code VARCHAR(128) NOT NULL,
                    event_name VARCHAR(128) NOT NULL,
                    source_state_id VARCHAR(128) NOT NULL,
                    target_state_id VARCHAR(128) NOT NULL,
                    condition_summary VARCHAR(512),
                    action_summary VARCHAR(512),
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        if (definitionJson() == null) {
            StateMachineDefinition demo = demoDefinition();
            jdbcTemplate.update("""
                    INSERT INTO pyin_plugin_state_machine_definition
                        (machine_key, machine_name, definition_json, published_version, updated_at)
                    VALUES (?, ?, ?, 0, ?)
                    """, demo.machineKey(), demo.machineName(), serialize(demo), Timestamp.from(Instant.now()));
        }
    }

    public Map<String, Object> workspace() {
        StateMachineDefinition definition = definition();
        Map<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("definition", definition);
        workspace.put("publishedVersion", publishedVersion());
        workspace.put("debug", debugSnapshot(definition));
        return workspace;
    }

    @Transactional
    public Map<String, Object> save(DefinitionSaveRequest request) {
        StateMachineDefinition definition = normalize(request);
        validate(definition);
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                UPDATE pyin_plugin_state_machine_definition
                SET machine_name = ?, definition_json = ?, updated_at = ?
                WHERE machine_key = ?
                """, definition.machineName(), serialize(definition), now, ORDER_MACHINE_KEY);
        Map<String, Object> response = workspace();
        response.put("message", "草稿已保存");
        return response;
    }

    @Transactional
    public Map<String, Object> publish() {
        StateMachineDefinition definition = definition();
        validate(definition);
        int version = publishedVersion() + 1;
        Timestamp now = Timestamp.from(Instant.now());
        String snapshot = serialize(definition);
        jdbcTemplate.update("""
                INSERT INTO pyin_plugin_state_machine_version (machine_key, version_no, definition_json, published_at)
                VALUES (?, ?, ?, ?)
                """, ORDER_MACHINE_KEY, version, snapshot, now);
        jdbcTemplate.update("UPDATE pyin_plugin_state_machine_definition SET published_version = ?, updated_at = ? WHERE machine_key = ?",
                version, now, ORDER_MACHINE_KEY);
        Map<String, Object> response = workspace();
        response.put("message", "已发布版本 v" + version);
        return response;
    }

    @Transactional
    public Map<String, Object> resetDebugSession() {
        StateMachineDefinition definition = definition();
        String initialState = initialStateId(definition);
        Timestamp now = Timestamp.from(Instant.now());
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_state_machine_debug_session WHERE machine_key = ?", Integer.class, ORDER_MACHINE_KEY);
        if (existing != null && existing > 0) {
            jdbcTemplate.update("UPDATE pyin_plugin_state_machine_debug_session SET current_state_id = ?, updated_at = ? WHERE machine_key = ?",
                    initialState, now, ORDER_MACHINE_KEY);
        } else {
            jdbcTemplate.update("INSERT INTO pyin_plugin_state_machine_debug_session (machine_key, current_state_id, updated_at) VALUES (?, ?, ?)",
                    ORDER_MACHINE_KEY, initialState, now);
        }
        jdbcTemplate.update("DELETE FROM pyin_plugin_state_machine_debug_log WHERE machine_key = ?", ORDER_MACHINE_KEY);
        return debugSnapshot(definition);
    }

    @Transactional
    public Map<String, Object> dispatchDebugEvent(DebugEventRequest request) {
        if (request == null || !StringUtils.hasText(request.eventCode())) {
            throw invalid("请选择要触发的事件。");
        }
        StateMachineDefinition definition = definition();
        String source = currentStateId(definition);
        Transition transition = definition.transitions().stream()
                .filter(item -> source.equals(item.source()) && request.eventCode().trim().equals(item.eventCode()))
                .findFirst()
                .orElseThrow(() -> invalid("当前状态下不能触发该事件。"));
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("UPDATE pyin_plugin_state_machine_debug_session SET current_state_id = ?, updated_at = ? WHERE machine_key = ?",
                transition.target(), now, ORDER_MACHINE_KEY);
        jdbcTemplate.update("""
                INSERT INTO pyin_plugin_state_machine_debug_log
                    (machine_key, event_code, event_name, source_state_id, target_state_id, condition_summary, action_summary, result, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ORDER_MACHINE_KEY, transition.eventCode(), transition.eventName(), transition.source(), transition.target(),
                blankToNull(transition.condition()), blankToNull(transition.actions()), "SUCCESS", now);
        return debugSnapshot(definition);
    }

    private Map<String, Object> debugSnapshot(StateMachineDefinition definition) {
        String currentState = currentStateId(definition);
        Map<String, StateNode> nodes = nodesById(definition);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("currentStateId", currentState);
        snapshot.put("currentStateName", nodes.containsKey(currentState) ? nodes.get(currentState).name() : currentState);
        snapshot.put("availableEvents", definition.transitions().stream()
                .filter(item -> currentState.equals(item.source()) && StringUtils.hasText(item.eventCode()))
                .map(item -> Map.of("eventCode", item.eventCode(), "eventName", item.eventName(), "condition", defaultText(item.condition(), "—")))
                .toList());
        snapshot.put("logs", listLogs(nodes));
        return snapshot;
    }

    private List<Map<String, Object>> listLogs(Map<String, StateNode> nodes) {
        return jdbcTemplate.query("""
                SELECT id, event_code, event_name, source_state_id, target_state_id, condition_summary, action_summary, result, created_at
                FROM pyin_plugin_state_machine_debug_log
                WHERE machine_key = ?
                ORDER BY id DESC
                """, (resultSet, rowNum) -> logRow(resultSet, nodes), ORDER_MACHINE_KEY);
    }

    private Map<String, Object> logRow(ResultSet resultSet, Map<String, StateNode> nodes) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        String source = resultSet.getString("source_state_id");
        String target = resultSet.getString("target_state_id");
        row.put("id", resultSet.getLong("id"));
        row.put("eventCode", resultSet.getString("event_code"));
        row.put("eventName", resultSet.getString("event_name"));
        row.put("sourceState", nodes.containsKey(source) ? nodes.get(source).name() : source);
        row.put("targetState", nodes.containsKey(target) ? nodes.get(target).name() : target);
        row.put("condition", defaultText(resultSet.getString("condition_summary"), "—"));
        row.put("actions", defaultText(resultSet.getString("action_summary"), "—"));
        row.put("result", resultSet.getString("result"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        row.put("createdAt", createdAt == null ? null : createdAt.toInstant().toString());
        return row;
    }

    private String currentStateId(StateMachineDefinition definition) {
        String current = jdbcTemplate.query("SELECT current_state_id FROM pyin_plugin_state_machine_debug_session WHERE machine_key = ?",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null, ORDER_MACHINE_KEY);
        if (StringUtils.hasText(current) && nodesById(definition).containsKey(current)) {
            return current;
        }
        return initialStateId(definition);
    }

    private StateMachineDefinition definition() {
        String json = definitionJson();
        if (!StringUtils.hasText(json)) {
            throw invalid("状态机草稿不存在。");
        }
        try {
            return objectMapper.readValue(json, StateMachineDefinition.class);
        } catch (JsonProcessingException exception) {
            throw invalid("状态机草稿数据已损坏。");
        }
    }

    private String definitionJson() {
        return jdbcTemplate.query("SELECT definition_json FROM pyin_plugin_state_machine_definition WHERE machine_key = ?",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null, ORDER_MACHINE_KEY);
    }

    private int publishedVersion() {
        Integer version = jdbcTemplate.queryForObject("SELECT published_version FROM pyin_plugin_state_machine_definition WHERE machine_key = ?",
                Integer.class, ORDER_MACHINE_KEY);
        return version == null ? 0 : version;
    }

    private StateMachineDefinition normalize(DefinitionSaveRequest request) {
        if (request == null) {
            throw invalid("状态机定义不能为空。");
        }
        return new StateMachineDefinition(ORDER_MACHINE_KEY,
                defaultText(request.machineName(), "订单状态机"),
                request.nodes() == null ? List.of() : request.nodes(),
                request.transitions() == null ? List.of() : request.transitions());
    }

    private void validate(StateMachineDefinition definition) {
        if (definition.nodes().isEmpty()) {
            throw invalid("请至少添加一个状态。");
        }
        List<StateNode> initials = definition.nodes().stream().filter(item -> "INITIAL".equals(item.type())).toList();
        if (initials.size() != 1) {
            throw invalid("状态机必须且只能包含一个初始状态。");
        }
        Set<String> nodeIds = new HashSet<>();
        for (StateNode node : definition.nodes()) {
            if (!StringUtils.hasText(node.id()) || !StringUtils.hasText(node.name()) || !StringUtils.hasText(node.type())) {
                throw invalid("状态必须填写 ID、名称和类型。");
            }
            if (!nodeIds.add(node.id())) {
                throw invalid("状态 ID 不能重复：" + node.id());
            }
        }
        for (Transition transition : definition.transitions()) {
            if (!StringUtils.hasText(transition.source()) || !StringUtils.hasText(transition.target())
                    || !nodeIds.contains(transition.source()) || !nodeIds.contains(transition.target())) {
                throw invalid("连线必须指向已存在的状态。");
            }
            if (!initials.get(0).id().equals(transition.source()) && !StringUtils.hasText(transition.eventCode())) {
                throw invalid("普通状态迁移必须填写事件编码。");
            }
        }
        if (definition.transitions().stream().noneMatch(item -> initials.get(0).id().equals(item.source()))) {
            throw invalid("初始状态必须连接到一个可运行状态。");
        }
    }

    private String initialStateId(StateMachineDefinition definition) {
        StateNode initial = definition.nodes().stream().filter(item -> "INITIAL".equals(item.type())).findFirst()
                .orElseThrow(() -> invalid("未配置初始状态。"));
        return definition.transitions().stream().filter(item -> initial.id().equals(item.source())).map(Transition::target).findFirst()
                .orElseThrow(() -> invalid("初始状态未连接到可运行状态。"));
    }

    private Map<String, StateNode> nodesById(StateMachineDefinition definition) {
        Map<String, StateNode> result = new HashMap<>();
        definition.nodes().forEach(node -> result.put(node.id(), node));
        return result;
    }

    private StateMachineDefinition demoDefinition() {
        List<StateNode> nodes = List.of(
                new StateNode("order.start", "初始状态", "INITIAL", "订单流程入口", 54, 165),
                new StateNode("order.waitingPay", "待支付", "NORMAL", "等待用户完成支付", 170, 135),
                new StateNode("order.paid", "已支付", "NORMAL", "订单已支付，等待发货", 420, 135),
                new StateNode("order.shipped", "已发货", "NORMAL", "商品已发货", 420, 340),
                new StateNode("order.refunding", "退款中", "NORMAL", "正在处理退款申请", 690, 135),
                new StateNode("order.completed", "已完成", "FINAL", "订单流程完成", 420, 530),
                new StateNode("order.cancelled", "已取消", "FINAL", "订单超时取消", 170, 340),
                new StateNode("order.refunded", "已退款", "FINAL", "退款成功", 920, 135),
                new StateNode("order.payFailed", "支付失败", "FINAL", "退款处理失败", 690, 340));
        List<Transition> transitions = List.of(
                edge("start", "order.start", "order.waitingPay", "", "", "", ""),
                edge("paid", "order.waitingPay", "order.paid", "PAY_SUCCESS", "支付成功", "", "发送支付成功通知"),
                edge("timeout", "order.waitingPay", "order.cancelled", "PAY_TIMEOUT", "超时未支付（30分钟）", "", "关闭订单"),
                edge("ship", "order.paid", "order.shipped", "SHIP", "发货", "", "扣减库存，发送发货通知"),
                edge("complete", "order.shipped", "order.completed", "CONFIRM_RECEIPT", "确认收货", "", "增加积分"),
                edge("refund", "order.shipped", "order.refunding", "APPLY_REFUND", "申请退款", "未确认收货", "创建退款单"),
                edge("refund-success", "order.refunding", "order.refunded", "REFUND_SUCCESS", "退款成功", "", "退款处理"),
                edge("refund-failed", "order.refunding", "order.payFailed", "REFUND_FAILED", "退款失败", "", "记录退款失败原因"));
        return new StateMachineDefinition(ORDER_MACHINE_KEY, "订单状态机", nodes, transitions);
    }

    private Transition edge(String id, String source, String target, String eventCode, String eventName, String condition, String actions) {
        return new Transition(id, source, target, eventCode, eventName, condition, actions);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalid("状态机定义无法序列化。");
        }
    }

    private static String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }

    private record StateMachineDefinition(String machineKey, String machineName, List<StateNode> nodes,
                                          List<Transition> transitions) {
    }
}
