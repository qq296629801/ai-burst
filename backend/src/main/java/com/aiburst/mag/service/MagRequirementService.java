package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagPoolDecideRequest;
import com.aiburst.mag.dto.MagPoolItemCreateRequest;
import com.aiburst.mag.dto.MagPoolProductCloseRequest;
import com.aiburst.mag.dto.MagRequirementChangeAnalyzeRequest;
import com.aiburst.mag.dto.MagRequirementSaveRequest;
import com.aiburst.mag.entity.MagMessage;
import com.aiburst.mag.entity.MagRequirementDoc;
import com.aiburst.mag.entity.MagRequirementPoolItem;
import com.aiburst.mag.entity.MagRequirementRevision;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagMessageMapper;
import com.aiburst.mag.mapper.MagModuleMapper;
import com.aiburst.mag.mapper.MagRequirementDocMapper;
import com.aiburst.mag.mapper.MagRequirementPoolItemMapper;
import com.aiburst.mag.mapper.MagRequirementRevisionMapper;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import com.aiburst.rbac.service.PermissionCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagRequirementService {

    private final MagRequirementDocMapper docMapper;
    private final MagRequirementRevisionMapper revisionMapper;
    private final MagRequirementPoolItemMapper poolMapper;
    private final MagAccessHelper accessHelper;
    private final PermissionCacheService permissionCacheService;
    private final MagThreadMapper threadMapper;
    private final MagMessageMapper messageMapper;
    private final MagTaskMapper taskMapper;
    private final MagModuleMapper moduleMapper;
    private final ObjectMapper objectMapper;

    /**
     * 供产品 Agent 工具读取当前需求正文（截断），会话与数据范围限定在项目内。
     */
    @Transactional(readOnly = true)
    public String readRequirementDocExcerpt(Long projectId, Long userId, int maxChars) {
        accessHelper.requireMember(projectId, userId);
        Map<String, Object> doc = getDoc(projectId, userId);
        String content = Objects.toString(doc.get("content"), "");
        if (maxChars <= 0) {
            maxChars = 8000;
        }
        maxChars = Math.min(maxChars, 120_000);
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "\n...[truncated]";
    }

    /**
     * 将「待评审」的开发需求/变更建议写入需求池，锚定当前最新修订，供人工或 PM 后续决策。
     */
    @Transactional
    public Map<String, Object> submitRequirementPoolCandidateFromAgent(
            Long projectId,
            Long userId,
            String summary,
            String proposedMarkdown,
            String anchorJsonHint) {
        accessHelper.requireMember(projectId, userId);
        if (!StringUtils.hasText(summary)) {
            throw new IllegalArgumentException("summary required");
        }
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        MagRequirementRevision latest = ensureFirstRevisionIfAbsent(doc, userId);
        MagPoolItemCreateRequest req = new MagPoolItemCreateRequest();
        req.setRevisionId(latest.getId());
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("summary", summary.trim());
            payload.put(
                    "proposedMarkdown",
                    proposedMarkdown != null ? proposedMarkdown : "");
            payload.put("source", "PRODUCT_AGENT");
            req.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("payload json error");
        }
        req.setAnchorJson(StringUtils.hasText(anchorJsonHint) ? anchorJsonHint.trim() : "{}");
        return createPoolItem(projectId, req, userId);
    }

    /**
     * 需求池项须锚定某版修订。尚无修订时自动写入空正文 v1 并更新 current_version。
     */
    private MagRequirementRevision ensureFirstRevisionIfAbsent(MagRequirementDoc doc, Long userId) {
        MagRequirementRevision latest = revisionMapper.selectLatest(doc.getId());
        if (latest != null) {
            return latest;
        }
        MagRequirementRevision rev = new MagRequirementRevision();
        rev.setDocId(doc.getId());
        rev.setVersion(1);
        rev.setContent("");
        rev.setAuthorUserId(userId);
        try {
            revisionMapper.insert(rev);
            docMapper.updateCurrentVersion(doc.getId(), 1);
            return rev;
        } catch (Exception e) {
            MagRequirementRevision again = revisionMapper.selectLatest(doc.getId());
            if (again != null) {
                return again;
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e);
        }
    }

    public Map<String, Object> getDoc(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        MagRequirementRevision latest = revisionMapper.selectLatest(doc.getId());
        Map<String, Object> out = new HashMap<>();
        out.put("docId", doc.getId());
        out.put("currentVersion", doc.getCurrentVersion());
        out.put("updatedAt", doc.getUpdatedAt());
        if (latest != null) {
            out.put("version", latest.getVersion());
            out.put("content", latest.getContent());
            out.put("authorUserId", latest.getAuthorUserId());
            out.put("revisionCreatedAt", latest.getCreatedAt());
        } else {
            out.put("version", 0);
            out.put("content", "");
        }
        return out;
    }

    @Transactional
    public Map<String, Object> saveDoc(Long projectId, MagRequirementSaveRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        int next = doc.getCurrentVersion() + 1;
        MagRequirementRevision rev = new MagRequirementRevision();
        rev.setDocId(doc.getId());
        rev.setVersion(next);
        rev.setContent(req.getContent());
        rev.setAuthorUserId(userId);
        revisionMapper.insert(rev);
        docMapper.updateCurrentVersion(doc.getId(), next);
        Map<String, Object> out = new HashMap<>();
        out.put("version", next);
        out.put("revisionId", rev.getId());
        return out;
    }

    public List<Map<String, Object>> listPool(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        boolean hasDecide = permissionCacheService.getPermCodes(userId).contains("mag:pool:decide");
        String role = accessHelper.memberRole(projectId, userId);
        return poolMapper.selectByProjectId(projectId).stream()
                .filter(it -> accessHelper.canSeePoolItem(it, userId, role, hasDecide))
                .map(this::poolRow)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createPoolItem(Long projectId, MagPoolItemCreateRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagRequirementPoolItem it = new MagRequirementPoolItem();
        it.setProjectId(projectId);
        it.setState(MagConstants.POOL_PENDING_USER);
        it.setRevisionId(req.getRevisionId());
        it.setAnchorJson(req.getAnchorJson());
        it.setPayloadJson(req.getPayloadJson());
        it.setAssignedDeciderUserId(req.getAssignedDeciderUserId());
        poolMapper.insert(it);
        return poolRow(poolMapper.selectById(it.getId()));
    }

    @Transactional
    public void decide(Long poolItemId, MagPoolDecideRequest req, Long userId) {
        MagRequirementPoolItem item = poolMapper.selectById(poolItemId);
        if (item == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(item.getProjectId(), userId);
        String role = accessHelper.memberRole(item.getProjectId(), userId);
        boolean hasDecide = permissionCacheService.getPermCodes(userId).contains("mag:pool:decide");
        if (!accessHelper.canSeePoolItem(item, userId, role, hasDecide)) {
            throw new MagBusinessException(MagResultCode.MAG_POOL_DECIDE_NOT_ALLOWED);
        }
        if (!MagConstants.POOL_PENDING_USER.equals(item.getState())) {
            throw new MagBusinessException(MagResultCode.MAG_POOL_STATE_INVALID);
        }
        String newState = mapDecision(req.getDecision());
        item.setState(newState);
        if (StringUtils.hasText(req.getNote())) {
            String payload = item.getPayloadJson();
            String suffix = "{\"decisionNote\":\"" + escapeJson(req.getNote()) + "\"}";
            if (payload == null || payload.isBlank()) {
                item.setPayloadJson(suffix);
            } else {
                item.setPayloadJson(payload + "," + suffix);
            }
        }
        poolMapper.update(item);
    }

    @Transactional
    public void productClosePoolItem(Long poolItemId, MagPoolProductCloseRequest req, Long userId) {
        if (!permissionCacheService.getPermCodes(userId).contains("mag:req:edit")) {
            throw new MagBusinessException(MagResultCode.MAG_FORBIDDEN);
        }
        MagRequirementPoolItem item = poolMapper.selectById(poolItemId);
        if (item == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        accessHelper.requireMember(item.getProjectId(), userId);
        item.setState(MagConstants.POOL_CLOSED_BY_PRODUCT);
        String ext = req.getPayloadExtensionJson();
        String base = "{\"productConclusion\":\"" + escapeJson(req.getConclusionSummary()) + "\"}";
        if (StringUtils.hasText(ext)) {
            item.setPayloadJson((item.getPayloadJson() != null ? item.getPayloadJson() + "," : "") + base + "," + ext);
        } else {
            item.setPayloadJson((item.getPayloadJson() != null ? item.getPayloadJson() + "," : "") + base);
        }
        poolMapper.update(item);
    }

    public List<Map<String, Object>> listRevisions(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        return revisionMapper.listByDocId(doc.getId()).stream().map(this::revisionRow).collect(Collectors.toList());
    }

    public Map<String, Object> diffRevisions(Long projectId, int version1, int version2, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        MagRequirementRevision r1 = revisionMapper.selectByDocAndVersion(doc.getId(), version1);
        MagRequirementRevision r2 = revisionMapper.selectByDocAndVersion(doc.getId(), version2);
        if (r1 == null || r2 == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("version1", version1);
        out.put("version2", version2);
        out.put("same", Objects.equals(r1.getContent(), r2.getContent()));
        out.put("unifiedTextDiff", buildSimpleDiff(r1.getContent(), r2.getContent()));
        return out;
    }

    @Transactional
    public Map<String, Object> analyzeRequirementChange(Long projectId, MagRequirementChangeAnalyzeRequest req, Long userId) {
        accessHelper.requireMember(projectId, userId);
        String traceId = UUID.randomUUID().toString();
        List<Map<String, Object>> openTasks = taskMapper.selectByProjectId(projectId).stream()
                .filter(t -> !MagConstants.TASK_DONE.equals(t.getState()))
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("taskId", t.getId());
                    m.put("title", t.getTitle());
                    m.put("state", t.getState());
                    return m;
                })
                .collect(Collectors.toList());
        List<Map<String, Object>> modules = moduleMapper.selectByProjectId(projectId).stream()
                .map(mo -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("moduleId", mo.getId());
                    m.put("name", mo.getName());
                    return m;
                })
                .collect(Collectors.toList());
        MagThread coord = ensureCoordThread(projectId);
        MagMessage sys = new MagMessage();
        sys.setThreadId(coord.getId());
        sys.setSenderType("SYSTEM");
        sys.setSenderAgentId(null);
        sys.setContent("{\"kind\":\"REQ_CHANGE_ANALYZE\",\"traceId\":\"" + traceId + "\",\"summary\":\""
                + escapeJson(req.getChangeSummary()) + "\"}");
        messageMapper.insert(sys);
        Map<String, Object> out = new HashMap<>();
        out.put("traceId", traceId);
        out.put("openTasksSample", openTasks);
        out.put("modulesSample", modules);
        out.put("suggestion", "请项目经理 Agent 依据 traceId 组织主 Agent 重评工时与依赖（编排层消费本记录）");
        return out;
    }

    private MagThread ensureCoordThread(Long projectId) {
        List<MagThread> threads = threadMapper.selectByProjectId(projectId);
        for (MagThread t : threads) {
            if ("需求与派工协调".equals(t.getTitle())) {
                return t;
            }
        }
        MagThread t = new MagThread();
        t.setProjectId(projectId);
        t.setTitle("需求与派工协调");
        threadMapper.insert(t);
        return threadMapper.selectById(t.getId());
    }

    private Map<String, Object> revisionRow(MagRequirementRevision r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("version", r.getVersion());
        m.put("authorUserId", r.getAuthorUserId());
        m.put("createdAt", r.getCreatedAt());
        m.put("contentPreview", r.getContent() != null && r.getContent().length() > 200
                ? r.getContent().substring(0, 200) + "…" : r.getContent());
        return m;
    }

    private static String buildSimpleDiff(String a, String b) {
        if (Objects.equals(a, b)) {
            return "";
        }
        return "--- version A\n" + (a == null ? "" : a) + "\n+++ version B\n" + (b == null ? "" : b);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String mapDecision(String decision) {
        if (MagConstants.DECISION_APPROVE_AS_IS.equals(decision)) {
            return MagConstants.USER_CONFIRMED_OK;
        }
        if (MagConstants.DECISION_APPROVE_WITH_CHANGE.equals(decision)) {
            return MagConstants.USER_CONFIRMED_CHANGE;
        }
        if (MagConstants.DECISION_REJECT.equals(decision)) {
            return "USER_REJECTED";
        }
        if (MagConstants.DECISION_DEFER.equals(decision)) {
            return "CLOSED";
        }
        throw new MagBusinessException(MagResultCode.MAG_POOL_STATE_INVALID, "unknown decision");
    }

    private Map<String, Object> poolRow(MagRequirementPoolItem it) {
        Map<String, Object> m = new HashMap<>();
        if (it == null) {
            return m;
        }
        m.put("id", it.getId());
        m.put("projectId", it.getProjectId());
        m.put("state", it.getState());
        m.put("revisionId", it.getRevisionId());
        m.put("anchorJson", it.getAnchorJson());
        m.put("payloadJson", it.getPayloadJson());
        m.put("assignedDeciderUserId", it.getAssignedDeciderUserId());
        m.put("temporalWorkflowId", it.getTemporalWorkflowId());
        m.put("createdAt", it.getCreatedAt());
        m.put("updatedAt", it.getUpdatedAt());
        return m;
    }
}
