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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MagTaskDispatchGateServiceTest {

    private static final long PID = 1L;

    @Mock private MagTaskMapper taskMapper;
    @Mock private MagAgentMapper agentMapper;
    @Mock private MagRequirementDocMapper requirementDocMapper;
    @Mock private MagRequirementRevisionMapper requirementRevisionMapper;

    @InjectMocks private MagTaskDispatchGateService gateService;

    private MagRequirementDoc doc;

    @BeforeEach
    void setUp() {
        doc = new MagRequirementDoc();
        doc.setId(99L);
        doc.setProjectId(PID);
        doc.setCurrentVersion(0);
    }

    @Test
    void dispatch_backend_blocked_when_requirement_empty() {
        when(requirementDocMapper.selectByProjectId(PID)).thenReturn(doc);
        when(requirementRevisionMapper.selectLatest(doc.getId())).thenReturn(null);
        MagAgent be = agent(2L, "BACKEND");
        when(agentMapper.selectById(2L)).thenReturn(be);

        assertThatThrownBy(() -> gateService.validateAssigneeForDispatchOrReassign(PID, 2L))
                .isInstanceOf(MagBusinessException.class)
                .extracting("resultCode")
                .isEqualTo(MagResultCode.MAG_DISPATCH_REQUIREMENT_NOT_READY);
    }

    @Test
    void dispatch_product_allowed_when_requirement_empty() {
        when(requirementDocMapper.selectByProjectId(PID)).thenReturn(doc);
        when(requirementRevisionMapper.selectLatest(doc.getId())).thenReturn(null);
        MagAgent p = agent(3L, "PRODUCT");
        when(agentMapper.selectById(3L)).thenReturn(p);
        when(taskMapper.selectByProjectId(PID)).thenReturn(List.of());

        gateService.validateAssigneeForDispatchOrReassign(PID, 3L);
    }

    @Test
    void dispatch_second_product_blocked_when_first_open() {
        when(requirementDocMapper.selectByProjectId(PID)).thenReturn(doc);
        MagRequirementRevision rev = new MagRequirementRevision();
        rev.setContent("# x");
        when(requirementRevisionMapper.selectLatest(doc.getId())).thenReturn(rev);

        MagAgent p1 = agent(3L, "PRODUCT");
        when(agentMapper.selectById(4L)).thenReturn(p1);

        MagTask open = new MagTask();
        open.setState(MagConstants.TASK_IN_PROGRESS);
        open.setAssigneeAgentId(3L);
        when(taskMapper.selectByProjectId(PID)).thenReturn(List.of(open));
        MagAgent existing = agent(3L, "PRODUCT");
        when(agentMapper.selectById(3L)).thenReturn(existing);

        assertThatThrownBy(() -> gateService.validateAssigneeForDispatchOrReassign(PID, 4L))
                .isInstanceOf(MagBusinessException.class)
                .extracting("resultCode")
                .isEqualTo(MagResultCode.MAG_DISPATCH_PRODUCT_PIPELINE_BLOCKED);
    }

    @Test
    void dispatch_test_blocked_when_backend_open() {
        when(requirementDocMapper.selectByProjectId(PID)).thenReturn(doc);
        MagRequirementRevision rev = new MagRequirementRevision();
        rev.setContent("# spec");
        when(requirementRevisionMapper.selectLatest(doc.getId())).thenReturn(rev);

        MagTask openBe = new MagTask();
        openBe.setState(MagConstants.TASK_PENDING);
        openBe.setAssigneeAgentId(2L);
        when(taskMapper.selectByProjectId(PID)).thenReturn(List.of(openBe));
        when(agentMapper.selectById(2L)).thenReturn(agent(2L, "BACKEND"));
        when(agentMapper.selectById(5L)).thenReturn(agent(5L, "TEST"));

        assertThatThrownBy(() -> gateService.validateAssigneeForDispatchOrReassign(PID, 5L))
                .isInstanceOf(MagBusinessException.class)
                .extracting("resultCode")
                .isEqualTo(MagResultCode.MAG_DISPATCH_TEST_BLOCKED_BY_DEV);
    }

    @Test
    void ask_pm_blocked_for_backend_when_no_requirement() {
        when(requirementDocMapper.selectByProjectId(PID)).thenReturn(doc);
        when(requirementRevisionMapper.selectLatest(doc.getId())).thenReturn(null);
        when(agentMapper.selectById(2L)).thenReturn(agent(2L, "BACKEND"));

        assertThatThrownBy(() -> gateService.checkMainAgentMayRequestPmDispatch(PID, 2L))
                .isInstanceOf(MagBusinessException.class)
                .extracting("resultCode")
                .isEqualTo(MagResultCode.MAG_DISPATCH_REQUIREMENT_NOT_READY);
    }

    @Test
    void ask_pm_allowed_for_product_when_no_requirement() {
        when(agentMapper.selectById(3L)).thenReturn(agent(3L, "PRODUCT"));

        gateService.checkMainAgentMayRequestPmDispatch(PID, 3L);
    }

    private static MagAgent agent(long id, String role) {
        MagAgent a = new MagAgent();
        a.setId(id);
        a.setProjectId(PID);
        a.setRoleType(role);
        return a;
    }
}
