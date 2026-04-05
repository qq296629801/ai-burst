package com.aiburst.mag.service;

import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagConstants;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.entity.MagRequirementDoc;
import com.aiburst.mag.entity.MagRequirementRevision;
import com.aiburst.mag.entity.MagTask;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.mapper.MagRequirementDocMapper;
import com.aiburst.mag.mapper.MagRequirementRevisionMapper;
import com.aiburst.mag.mapper.MagTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 项目经理派工与「向 PM 要活」的流水线门禁：需求文档就绪、产品单飞、测试在开发结项之后。
 */
@Service
@RequiredArgsConstructor
public class MagTaskDispatchGateService {

    private final MagTaskMapper taskMapper;
    private final MagAgentMapper agentMapper;
    private final MagRequirementDocMapper requirementDocMapper;
    private final MagRequirementRevisionMapper requirementRevisionMapper;

    /**
     * 新建派工、带指派人的任务创建、项目经理改派的新执行人，均须通过本校验。
     */
    @Transactional(readOnly = true)
    public void validateAssigneeForDispatchOrReassign(long projectId, long assigneeAgentId) {
        MagAgent assignee = agentMapper.selectById(assigneeAgentId);
        if (assignee == null || !Objects.equals(assignee.getProjectId(), projectId)) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        String role = assignee.getRoleType();
        boolean reqReady = hasNonEmptyRequirementContent(projectId);

        if (!reqReady) {
            if (!"PRODUCT".equals(role)) {
                throw new MagBusinessException(
                        MagResultCode.MAG_DISPATCH_REQUIREMENT_NOT_READY,
                        "需求文档尚无有效正文：首次派工仅能指派产品（PRODUCT）Agent；待产品产出并写入需求后再派开发/测试等职能。");
            }
        }

        if ("PRODUCT".equals(role)) {
            if (countOpenTasksForRole(projectId, "PRODUCT") > 0) {
                throw new MagBusinessException(
                        MagResultCode.MAG_DISPATCH_PRODUCT_PIPELINE_BLOCKED,
                        "产品（PRODUCT）职能尚有未结项任务（待处理/进行中/阻塞），不得再向产品 Agent 派工。");
            }
        }

        if ("TEST".equals(role)) {
            if (countOpenTasksForRoles(projectId, "FRONTEND", "BACKEND") > 0) {
                throw new MagBusinessException(
                        MagResultCode.MAG_DISPATCH_TEST_BLOCKED_BY_DEV,
                        "前端/后端尚有未结项任务或未完成产出闭环：须待对应开发任务均已结项后，方可向测试（TEST）Agent 派工。");
            }
        }
    }

    /**
     * 本子线主 Agent 调用 {@code mag_ask_pm_for_next_tasks} 前的门禁（产品职能除外）。
     */
    @Transactional(readOnly = true)
    public void checkMainAgentMayRequestPmDispatch(long projectId, long callerAgentId) {
        MagAgent self = agentMapper.selectById(callerAgentId);
        if (self == null || !Objects.equals(self.getProjectId(), projectId)) {
            throw new MagBusinessException(MagResultCode.MAG_NOT_FOUND);
        }
        if ("PRODUCT".equals(self.getRoleType())) {
            return;
        }
        boolean reqReady = hasNonEmptyRequirementContent(projectId);
        if (!reqReady) {
            throw new MagBusinessException(
                    MagResultCode.MAG_DISPATCH_REQUIREMENT_NOT_READY,
                    "需求文档尚无有效正文前，仅产品（PRODUCT）职能主 Agent 可向项目经理请求派工。");
        }
        if ("TEST".equals(self.getRoleType())) {
            if (countOpenTasksForRoles(projectId, "FRONTEND", "BACKEND") > 0) {
                throw new MagBusinessException(
                        MagResultCode.MAG_DISPATCH_TEST_BLOCKED_BY_DEV,
                        "前端/后端尚有未结项任务时，测试（TEST）职能主 Agent 不可向项目经理要派工。");
            }
        }
    }

    /** 最新需求修订存在且正文非空（trim 后）。 */
    @Transactional(readOnly = true)
    public boolean hasNonEmptyRequirementContent(long projectId) {
        MagRequirementDoc doc = requirementDocMapper.selectByProjectId(projectId);
        if (doc == null) {
            return false;
        }
        MagRequirementRevision latest = requirementRevisionMapper.selectLatest(doc.getId());
        if (latest == null || latest.getContent() == null) {
            return false;
        }
        return StringUtils.hasText(latest.getContent().trim());
    }

    private static boolean isOpenTaskState(String state) {
        return MagConstants.TASK_PENDING.equals(state)
                || MagConstants.TASK_IN_PROGRESS.equals(state)
                || MagConstants.TASK_BLOCKED.equals(state);
    }

    private long countOpenTasksForRole(long projectId, String roleType) {
        List<MagTask> tasks = taskMapper.selectByProjectId(projectId);
        if (tasks == null) {
            return 0;
        }
        long n = 0;
        for (MagTask t : tasks) {
            if (!isOpenTaskState(t.getState())) {
                continue;
            }
            Long aid = t.getAssigneeAgentId();
            if (aid == null) {
                continue;
            }
            MagAgent a = agentMapper.selectById(aid);
            if (a != null && roleType.equals(a.getRoleType())) {
                n++;
            }
        }
        return n;
    }

    private long countOpenTasksForRoles(long projectId, String roleA, String roleB) {
        List<MagTask> tasks = taskMapper.selectByProjectId(projectId);
        if (tasks == null) {
            return 0;
        }
        long n = 0;
        for (MagTask t : tasks) {
            if (!isOpenTaskState(t.getState())) {
                continue;
            }
            Long aid = t.getAssigneeAgentId();
            if (aid == null) {
                continue;
            }
            MagAgent a = agentMapper.selectById(aid);
            if (a == null) {
                continue;
            }
            String r = a.getRoleType();
            if (roleA.equals(r) || roleB.equals(r)) {
                n++;
            }
        }
        return n;
    }
}
