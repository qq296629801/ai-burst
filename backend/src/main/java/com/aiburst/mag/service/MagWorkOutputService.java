package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.entity.MagAgentImprovementLog;
import com.aiburst.mag.mapper.MagAgentImprovementLogMapper;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聚合展示 Agent 干活产出：改进日志（开发实现说明、测试计划等）、需求池候选、需求文档版本快照。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagWorkOutputService {

    private static final int IMP_MAX = 500;
    private static final int POOL_MAX = 200;
    private static final int REV_MAX = 100;

    private final MagAccessHelper accessHelper;
    private final MagAgentImprovementLogMapper improvementLogMapper;
    private final MagAgentMapper agentMapper;
    private final MagRequirementService requirementService;
    private final ObjectMapper objectMapper;

    /**
     * @return {@code items}：按时间倒序的统一列表，每条含 {@code kind}、{@code occurredAt}、{@code summary}、{@code body} 等
     */
    public Map<String, Object> listAggregated(
            long projectId, long userId, int improvementLimit, int poolLimit, int revisionLimit) {
        accessHelper.requireMember(projectId, userId);
        int impCap = Math.min(Math.max(improvementLimit, 1), IMP_MAX);
        int poolCap = Math.min(Math.max(poolLimit, 1), POOL_MAX);
        int revCap = Math.min(Math.max(revisionLimit, 1), REV_MAX);

        Map<Long, MagAgent> agentsById =
                agentMapper.selectByProjectId(projectId).stream()
                        .collect(Collectors.toMap(MagAgent::getId, a -> a, (a, b) -> a));

        List<Map<String, Object>> items = new ArrayList<>();

        for (MagAgentImprovementLog log : improvementLogMapper.selectByProjectId(projectId, impCap)) {
            MagAgent ag = agentsById.get(log.getAgentId());
            Map<String, Object> m = new HashMap<>();
            m.put("kind", "IMPROVEMENT");
            m.put("recordId", log.getId());
            m.put("occurredAt", log.getCreatedAt());
            m.put("agentId", log.getAgentId());
            m.put("agentName", ag != null ? ag.getName() : null);
            m.put("agentRoleType", ag != null ? ag.getRoleType() : null);
            m.put("changeType", log.getChangeType());
            m.put("summary", log.getSummary());
            m.put("body", log.getDetailJson());
            m.put("createdByUserId", log.getCreatedByUserId());
            items.add(m);
        }

        List<Map<String, Object>> poolRows;
        try {
            poolRows = requirementService.listPool(projectId, userId);
        } catch (Exception e) {
            log.debug("work-outputs pool list skipped: {}", e.toString());
            poolRows = List.of();
        }
        int pi = 0;
        for (Map<String, Object> row : poolRows) {
            if (pi++ >= poolCap) {
                break;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("kind", "REQUIREMENT_POOL");
            m.put("recordId", row.get("id"));
            m.put("occurredAt", row.get("createdAt"));
            m.put("state", row.get("state"));
            m.put("agentId", null);
            m.put("agentName", null);
            m.put("agentRoleType", "PRODUCT");
            m.put("changeType", "REQUIREMENT_POOL_ITEM");
            m.put("summary", poolPayloadSummary(row.get("payloadJson")));
            m.put("body", prettyPayload(row.get("payloadJson")));
            items.add(m);
        }

        List<Map<String, Object>> revRows = List.of();
        try {
            revRows = requirementService.listRevisions(projectId, userId);
        } catch (MagBusinessException ex) {
            if (ex.getResultCode() != MagResultCode.MAG_NOT_FOUND) {
                throw ex;
            }
        }
        int ri = 0;
        for (Map<String, Object> r : revRows) {
            if (ri++ >= revCap) {
                break;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("kind", "REQUIREMENT_DOC");
            m.put("recordId", r.get("id"));
            m.put("revisionVersion", r.get("version"));
            m.put("occurredAt", r.get("createdAt"));
            m.put("authorUserId", r.get("authorUserId"));
            m.put("agentId", null);
            m.put("agentName", "需求文档");
            m.put("agentRoleType", null);
            m.put("changeType", "REQUIREMENT_REVISION");
            m.put("summary", "需求文档版本 v" + r.get("version"));
            m.put("body", r.get("contentPreview"));
            items.add(m);
        }

        items.sort(
                Comparator.comparing(
                        (Map<String, Object> x) -> asLocalDateTime(x.get("occurredAt")),
                        Comparator.nullsLast(Comparator.reverseOrder())));

        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        return out;
    }

    private static LocalDateTime asLocalDateTime(Object o) {
        if (o instanceof LocalDateTime ldt) {
            return ldt;
        }
        return null;
    }

    private String poolPayloadSummary(Object payloadJson) {
        if (payloadJson == null) {
            return "需求池项";
        }
        try {
            JsonNode n = objectMapper.readTree(String.valueOf(payloadJson));
            if (n.has("summary") && n.get("summary").isTextual()) {
                return n.get("summary").asText();
            }
        } catch (Exception e) {
            log.trace("pool payload parse: {}", e.toString());
        }
        String s = String.valueOf(payloadJson);
        return s.length() > 48 ? "需求池项 · " + s.substring(0, 45) + "…" : "需求池项 · " + s;
    }

    private String prettyPayload(Object payloadJson) {
        if (payloadJson == null) {
            return "";
        }
        String raw = String.valueOf(payloadJson);
        try {
            JsonNode n = objectMapper.readTree(raw);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(n);
        } catch (Exception e) {
            return raw;
        }
    }
}
