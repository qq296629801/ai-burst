package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.dto.MagRequirementChangeAnalyzeRequest;
import com.aiburst.mag.dto.MagRequirementSaveRequest;
import com.aiburst.mag.entity.MagMessage;
import com.aiburst.mag.entity.MagRequirementDoc;
import com.aiburst.mag.entity.MagRequirementRevision;
import com.aiburst.mag.entity.MagThread;
import com.aiburst.mag.mapper.MagMessageMapper;
import com.aiburst.mag.mapper.MagModuleMapper;
import com.aiburst.mag.mapper.MagRequirementDocMapper;
import com.aiburst.mag.mapper.MagRequirementRevisionMapper;
import com.aiburst.mag.mapper.MagTaskMapper;
import com.aiburst.mag.mapper.MagThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final MagAccessHelper accessHelper;
    private final MagThreadMapper threadMapper;
    private final MagMessageMapper messageMapper;
    private final MagTaskMapper taskMapper;
    private final MagModuleMapper moduleMapper;

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
     * 产品 Agent：将「开发侧需求说明」直接合并进需求文档新版本（不经需求池）。
     */
    @Transactional
    public Map<String, Object> mergeDevRequirementProposedFromAgent(
            Long projectId, Long userId, String summary, String proposedMarkdown, String anchorJsonHint) {
        accessHelper.requireMember(projectId, userId);
        if (!StringUtils.hasText(summary)) {
            throw new IllegalArgumentException("summary required");
        }
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        ensureFirstRevisionIfAbsent(doc, userId);
        Long newRevId = null;
        if (StringUtils.hasText(proposedMarkdown)) {
            newRevId = appendMergedRequirementRevision(projectId, proposedMarkdown, userId);
        } else {
            // 仅有 summary 时也落一版修订，便于产品任务在「已提交合并工具」路径上拿到 revisionId 并自动结项
            String body = "### 产品摘要（Agent）\n\n" + summary.trim();
            newRevId = appendMergedRequirementRevision(projectId, body, userId);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("revisionId", newRevId);
        out.put("summary", summary.trim());
        if (StringUtils.hasText(anchorJsonHint)) {
            out.put("anchorJson", anchorJsonHint.trim());
        }
        return out;
    }

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

    public List<Map<String, Object>> listRevisions(Long projectId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        return revisionMapper.listByDocId(doc.getId()).stream().map(this::revisionRow).collect(Collectors.toList());
    }

    /**
     * 单条修订全文（Markdown），用于版本列表「预览」弹窗；校验修订属于本项目需求文档。
     */
    public Map<String, Object> getRevision(Long projectId, Long revisionId, Long userId) {
        accessHelper.requireMember(projectId, userId);
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        MagRequirementRevision r = revisionMapper.selectById(revisionId);
        if (r == null || !doc.getId().equals(r.getDocId())) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("version", r.getVersion());
        m.put("content", r.getContent() != null ? r.getContent() : "");
        m.put("authorUserId", r.getAuthorUserId());
        m.put("createdAt", r.getCreatedAt());
        return m;
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

    /**
     * 将片段合并进需求文档最新正文并新增修订版本；片段为空则不写入。
     *
     * @return 新修订 id，未写入则 {@code null}
     */
    private Long appendMergedRequirementRevision(Long projectId, String fragmentMarkdown, Long userId) {
        if (!StringUtils.hasText(fragmentMarkdown)) {
            return null;
        }
        MagRequirementDoc doc = docMapper.selectByProjectId(projectId);
        if (doc == null) {
            return null;
        }
        MagRequirementRevision latest = revisionMapper.selectLatest(doc.getId());
        String base = latest == null || latest.getContent() == null ? "" : latest.getContent().trim();
        String fragment = fragmentMarkdown.trim();
        String newContent = base.isEmpty() ? fragment : base + "\n\n---\n\n" + fragment;
        int next = doc.getCurrentVersion() + 1;
        MagRequirementRevision rev = new MagRequirementRevision();
        rev.setDocId(doc.getId());
        rev.setVersion(next);
        rev.setContent(newContent);
        rev.setAuthorUserId(userId);
        revisionMapper.insert(rev);
        docMapper.updateCurrentVersion(doc.getId(), next);
        return rev.getId();
    }
}
