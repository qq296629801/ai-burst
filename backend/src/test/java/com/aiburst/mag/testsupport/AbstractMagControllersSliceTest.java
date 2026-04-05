package com.aiburst.mag.testsupport;

import com.aiburst.mag.slice.MagApiControllersSliceApplication;
import com.aiburst.mag.service.MagAgentService;
import com.aiburst.mag.service.MagAlertService;
import com.aiburst.mag.service.MagCollaborationService;
import com.aiburst.mag.service.MagDashboardService;
import com.aiburst.mag.service.MagFetchAuditService;
import com.aiburst.mag.service.MagImprovementLogService;
import com.aiburst.mag.service.MagKbService;
import com.aiburst.mag.service.MagMemberService;
import com.aiburst.mag.service.MagModuleService;
import com.aiburst.mag.service.MagOrchestrationRunService;
import com.aiburst.mag.service.MagPmAssistService;
import com.aiburst.mag.service.MagProjectService;
import com.aiburst.mag.service.MagReleaseService;
import com.aiburst.mag.service.MagRequirementService;
import com.aiburst.mag.service.MagScheduledJobService;
import com.aiburst.mag.service.MagTaskService;
import com.aiburst.mag.service.MagTodoService;
import com.aiburst.mag.service.MagWorkOutputService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * 为 {@link MagApiControllersSliceApplication} 下全部 MAG 控制器提供 Service Mock。
 */
@SpringBootTest(classes = MagApiControllersSliceApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public abstract class AbstractMagControllersSliceTest {

    @MockBean
    protected MagProjectService projectService;

    @MockBean
    protected MagMemberService memberService;

    @MockBean
    protected MagTaskService taskService;

    @MockBean
    protected MagAgentService agentService;

    @MockBean
    protected MagRequirementService requirementService;

    @MockBean
    protected MagKbService kbService;

    @MockBean
    protected MagModuleService moduleService;

    @MockBean
    protected MagOrchestrationRunService orchestrationRunService;

    @MockBean
    protected MagCollaborationService collaborationService;

    @MockBean
    protected MagAlertService alertService;

    @MockBean
    protected MagScheduledJobService scheduledJobService;

    @MockBean
    protected MagFetchAuditService fetchAuditService;

    @MockBean
    protected MagPmAssistService pmAssistService;

    @MockBean
    protected MagImprovementLogService improvementLogService;

    @MockBean
    protected MagDashboardService dashboardService;

    @MockBean
    protected MagTodoService todoService;

    @MockBean
    protected MagWorkOutputService workOutputService;

    @MockBean
    protected MagReleaseService releaseService;
}
