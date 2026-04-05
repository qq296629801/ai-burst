package com.aiburst.mag.agentscope;

import com.aiburst.llm.catalog.LlmProviderCatalog;
import com.aiburst.llm.crypto.LlmCryptoService;
import com.aiburst.llm.entity.LlmChannel;
import com.aiburst.llm.mapper.LlmChannelMapper;
import com.aiburst.llm.model.LlmProtocol;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.entity.MagAgent;
import com.aiburst.mag.mapper.MagAgentMapper;
import com.aiburst.mag.service.MagCoordinationChatWriter;
import com.aiburst.mag.service.MagModuleService;
import com.aiburst.mag.service.MagRequirementService;
import com.aiburst.mag.service.MagTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelException;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import io.agentscope.core.tool.subagent.SubAgentProvider;
import io.agentscope.core.tool.subagent.SubAgentTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.Exceptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Temporal Activity 内通过 AgentScope Java 调用 {@link MagAgent} 绑定的大模型通道；
 * 会话持久化目录按 projectId + agentId 隔离；按角色注册工具（PM 派工、产品需求、开发分层、测试单测计划、A2A、
 * 主 Agent 向 PM 要派工 {@link MagMainAgentPmRequestTools}、可选 MCP 与 classpath Agent Skill、PM 子 Agent as Tool）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagAgentScopeRunService {

    private static final ObjectMapper MODEL_ERROR_JSON = new ObjectMapper();

    private static final ThreadLocal<Integer> A2A_DEPTH = ThreadLocal.withInitial(() -> 0);

    private final LlmChannelMapper llmChannelMapper;
    private final LlmCryptoService llmCryptoService;
    private final LlmProviderCatalog llmProviderCatalog;
    private final MagTaskService magTaskService;
    private final MagModuleService magModuleService;
    private final MagAgentMapper magAgentMapper;
    private final MagRequirementService magRequirementService;
    private final MagMcpToolRegistry magMcpToolRegistry;
    private final MagAgentScopeSerialRunner serialRunner;
    private final MagCoordinationChatWriter coordinationChatWriter;

    @Value("${aiburst.mag.agentscope.serial-agent-run:true}")
    private boolean serialAgentRun;

    /**
     * 含排队等待 + 单次编排 wall-clock 上限（秒）；应大于 {@link #callTimeoutSeconds}。
     */
    @Value("${aiburst.mag.agentscope.serial-queue-timeout-seconds:600}")
    private int serialQueueTimeoutSeconds;

    @Value("${aiburst.mag.agentscope.max-iters:4}")
    private int maxIters;

    @Value("${aiburst.mag.agentscope.pm-max-iters:10}")
    private int pmMaxIters;

    @Value("${aiburst.mag.agentscope.call-timeout-seconds:120}")
    private int callTimeoutSeconds;

    /**
     * Reactor 对整段 ReAct 流 {@code collectList().block(timeout)} 的墙钟上限（秒）。
     * 0 表示按单次 LLM 超时 × ReAct 迭代次数 × A2A 深度预算自动估算（避免嵌套 invoke_peer / PM 编排时过早超时）。
     */
    @Value("${aiburst.mag.agentscope.stream-block-timeout-seconds:0}")
    private int streamBlockTimeoutSeconds;

    @Value("${aiburst.mag.agentscope.session-root:${java.io.tmpdir}/mag-agent-sessions}")
    private String sessionRoot;

    @Value("${aiburst.mag.agentscope.a2a-max-depth:5}")
    private int a2aMaxDepth;

    @Value("${aiburst.mag.skills.enabled:true}")
    private boolean skillsEnabled;

    @Value("${aiburst.mag.skills.classpath-root:mag-skills}")
    private String skillsClasspathRoot;

    /**
     * 执行一次 ReAct 编排；支持由 {@link MagPeerInvokeTools} 触发的同项目嵌套调用（Agent2Agent）。
     * <p>根级调用（非嵌套）在 {@link #serialAgentRun} 为 true 时进入单线程 FIFO 队列依次执行，避免多 Workflow 并发打满通道。
     * 嵌套调用与队列工作线程同线程，不再入队，避免死锁。
     */
    public String executeAgentRun(MagAgent agent, long triggerUserId, String instruction) {
        return executeAgentRun(agent, triggerUserId, instruction, null);
    }

    /**
     * @param taskContextTaskId 非空时写入对应任务沟通线程的气泡与协调消息；嵌套 A2A 沿用根级 ThreadLocal
     */
    public String executeAgentRun(MagAgent agent, long triggerUserId, String instruction, Long taskContextTaskId) {
        int d = A2A_DEPTH.get();
        if (d >= a2aMaxDepth) {
            throw new MagBusinessException(MagResultCode.MAG_UNKNOWN, "Agent2Agent 嵌套深度超过限制");
        }
        if (serialAgentRun && d == 0) {
            try {
                Long capTask = taskContextTaskId;
                return serialRunner
                        .submit(() -> executeAgentRunWithinDepth(agent, triggerUserId, instruction, capTask))
                        .get(Math.max(serialQueueTimeoutSeconds, callTimeoutSeconds + 60), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new MagBusinessException(
                        MagResultCode.MAG_UNKNOWN,
                        "Agent 编排队列等待/执行超时（serial-queue-timeout-seconds=" + serialQueueTimeoutSeconds + "）");
            } catch (ExecutionException e) {
                Throwable c = e.getCause();
                if (c instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException(c != null ? c.getMessage() : "serial run failed", c);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Agent 编排队列被中断", e);
            }
        }
        return executeAgentRunWithinDepth(agent, triggerUserId, instruction, taskContextTaskId);
    }

    private String executeAgentRunWithinDepth(
            MagAgent agent, long triggerUserId, String instruction, Long rootTaskContextTaskId) {
        int depth = A2A_DEPTH.get();
        if (depth >= a2aMaxDepth) {
            throw new MagBusinessException(MagResultCode.MAG_UNKNOWN, "Agent2Agent 嵌套深度超过限制");
        }
        if (depth == 0) {
            MagAgentRunTaskContext.set(rootTaskContextTaskId);
        }
        try {
            Long taskForThread = MagAgentRunTaskContext.get();
            if (depth >= 1) {
                Long callerAgentId = MagA2aCallerStack.peek();
                if (callerAgentId != null) {
                    try {
                        coordinationChatWriter.recordA2aInvoke(
                                agent.getProjectId(),
                                callerAgentId,
                                agent.getId(),
                                triggerUserId,
                                instruction != null ? instruction : "",
                                taskForThread);
                    } catch (Exception ex) {
                        log.warn("MAG A2A 写入沟通失败: {}", ex.toString());
                    }
                }
            } else {
                try {
                    coordinationChatWriter.recordOrchestrationEnter(
                            agent.getProjectId(),
                            agent.getId(),
                            triggerUserId,
                            instruction != null ? instruction : "",
                            taskForThread);
                } catch (Exception ex) {
                    log.warn("MAG 编排进入写入沟通失败: {}", ex.toString());
                }
            }
            A2A_DEPTH.set(depth + 1);
            try {
                return runOnceWithProjectSession(agent, triggerUserId, instruction);
            } finally {
                A2A_DEPTH.set(depth);
            }
        } finally {
            if (depth == 0) {
                MagAgentRunTaskContext.clear();
            }
        }
    }

    private String runOnceWithProjectSession(MagAgent agent, long triggerUserId, String instruction) {
        if (agent.getLlmChannelId() == null) {
            throw new MagBusinessException(
                    MagResultCode.MAG_AGENT_LLM_CHANNEL_REQUIRED,
                    "Agent 未绑定大模型通道（llmChannelId），无法执行编排");
        }
        LlmChannel ch = llmChannelMapper.selectByIdAndOwner(agent.getLlmChannelId(), triggerUserId);
        if (ch == null) {
            throw new MagBusinessException(
                    MagResultCode.MAG_NOT_FOUND, "大模型通道不存在或触发用户不是通道所有者");
        }
        String apiKey = llmCryptoService.decrypt(ch.getApiKeyCipher());
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("channel api key empty after decrypt");
        }
        String model = StringUtils.hasText(ch.getDefaultModel()) ? ch.getDefaultModel().trim() : null;
        if (!StringUtils.hasText(model)) {
            throw new IllegalArgumentException("通道未配置默认模型（default_model）");
        }

        LlmProtocol protocol = LlmProtocol.valueOf(ch.getProtocol());
        GenerateOptions gen = generateOptions();
        Model chatModel = buildChatModel(ch, apiKey, model, protocol);

        boolean isPm = "PM".equals(agent.getRoleType());
        String sysPrompt = buildSysPrompt(agent, isPm);
        int iters = isPm ? pmMaxIters : maxIters;

        Toolkit toolkit = new Toolkit();
        configureToolkit(toolkit, agent, triggerUserId, chatModel, gen, isPm, iters);
        magMcpToolRegistry.registerInto(toolkit);

        ReActAgent.Builder agentBuilder =
                ReActAgent.builder()
                        .name("mag-" + agent.getId())
                        .sysPrompt(sysPrompt)
                        .model(chatModel)
                        .generateOptions(gen)
                        .maxIters(iters)
                        .enableMetaTool(false)
                        .memory(new InMemoryMemory())
                        .toolkit(toolkit);

        attachSkillBoxIfEnabled(agentBuilder, toolkit);

        ReActAgent reactAgent = agentBuilder.build();

        Path sessionDir = projectAgentSessionDir(agent.getProjectId(), agent.getId());
        try {
            Files.createDirectories(sessionDir);
        } catch (Exception e) {
            throw new IllegalStateException("session dir: " + sessionDir, e);
        }
        JsonSession jsonSession = new JsonSession(sessionDir);
        SimpleSessionKey sessionKey = SimpleSessionKey.of("mag-main");
        reactAgent.loadIfExists(jsonSession, sessionKey);

        String userLine = buildUserLine(isPm, agent, instruction);

        long awaitSeconds = resolveStreamBlockTimeoutSeconds(iters);
        List<Msg> input = List.of(Msg.builder().role(MsgRole.USER).textContent(userLine).build());
        List<Event> events;
        try {
            events =
                    reactAgent
                            .stream(input, StreamOptions.defaults())
                            .collectList()
                            .block(Duration.ofSeconds(awaitSeconds));
        } catch (Exception e) {
            Throwable root = Exceptions.unwrap(e);
            ModelException me = unwrapModelException(root);
            if (me != null) {
                String summary = summarizeModelException(me);
                log.warn(
                        "MAG ModelException agentId={} projectId={}: {}",
                        agent.getId(),
                        agent.getProjectId(),
                        summary);
                throw new IllegalStateException(summary, me);
            }
            if (root instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(
                    root != null ? root.getMessage() : "AgentScope stream failed", root);
        }

        if (events == null) {
            events = List.of();
        }

        long chatThreadId =
                coordinationChatWriter.resolveThreadIdForCoordOrPmTask(
                        agent.getProjectId(), MagAgentRunTaskContext.get());
        try {
            coordinationChatWriter.appendAgentScopeChatTurns(chatThreadId, agent.getId(), events);
        } catch (Exception ex) {
            log.warn("MAG 沟通气泡落库失败 agentId={}: {}", agent.getId(), ex.toString());
        }

        try {
            reactAgent.saveTo(jsonSession, sessionKey);
        } catch (Exception e) {
            log.warn("MAG session save failed agentId={}: {}", agent.getId(), e.toString());
        }

        Msg reply = null;
        for (int i = events.size() - 1; i >= 0; i--) {
            Event ev = events.get(i);
            Msg m = ev != null ? ev.getMessage() : null;
            if (m != null
                    && m.getRole() == MsgRole.ASSISTANT
                    && StringUtils.hasText(m.getTextContent())) {
                reply = m;
                break;
            }
        }
        String text =
                reply != null && StringUtils.hasText(reply.getTextContent())
                        ? reply.getTextContent().trim()
                        : "";
        log.info(
                "MAG AgentScope agentId={} projectId={} pm={} replyChars={}",
                agent.getId(),
                agent.getProjectId(),
                isPm,
                text.length());
        return text.isEmpty() ? "OK" : text;
    }

    private Path projectAgentSessionDir(long projectId, long agentId) {
        Path root = Paths.get(sessionRoot).toAbsolutePath().normalize();
        return root.resolve("p" + projectId).resolve("a" + agentId);
    }

    private void configureToolkit(
            Toolkit toolkit,
            MagAgent agent,
            long triggerUserId,
            Model chatModel,
            GenerateOptions gen,
            boolean isPm,
            int iters) {
        long projectId = agent.getProjectId();
        long agentId = agent.getId();
        String role = agent.getRoleType() != null ? agent.getRoleType() : "";

        toolkit.registerTool(
                new MagPeerInvokeTools(
                        projectId, triggerUserId, agentId, magAgentMapper, this::executeAgentRun));

        if (!isPm && agent.getParentAgentId() == null) {
            toolkit.registerTool(
                    new MagMainAgentPmRequestTools(
                            projectId, triggerUserId, agentId, magAgentMapper, this::executeAgentRun));
        }

        if (isPm) {
            toolkit.registerTool(
                    new MagPmDispatchTools(
                            projectId, triggerUserId, agentId, magTaskService, magModuleService, magAgentMapper));
            SubAgentConfig subCfg =
                    SubAgentConfig.builder()
                            .toolName("pm_delegate_reflection")
                            .description(
                                    "Agent as Tool：无工具子 Agent，对派工/需求问题进行一步结构化归纳，"
                                            + "供项目经理再继续调用 list_dispatchable_agents、dispatch_task。")
                            .build();
            toolkit.registerTool(
                    new SubAgentTool(
                            (SubAgentProvider<ReActAgent>)
                                    () ->
                                            ReActAgent.builder()
                                                    .name("pm-sub-reflection")
                                                    .sysPrompt(
                                                            "你是项目经理的子助理（Agent as Tool），只做简短条目式归纳，"
                                                                    + "不调用工具、不直接派工。")
                                                    .model(chatModel)
                                                    .generateOptions(gen)
                                                    .maxIters(Math.min(3, iters))
                                                    .enableMetaTool(false)
                                                    .memory(new InMemoryMemory())
                                                    .build(),
                            subCfg));
        }

        if ("PRODUCT".equals(role)) {
            toolkit.registerTool(
                    new MagProductRequirementTools(projectId, triggerUserId, magRequirementService));
        }
    }

    private void attachSkillBoxIfEnabled(ReActAgent.Builder agentBuilder, Toolkit toolkit) {
        if (!skillsEnabled) {
            return;
        }
        try {
            ClasspathSkillRepository repo = new ClasspathSkillRepository(skillsClasspathRoot);
            SkillBox box = new SkillBox(toolkit);
            for (String name : repo.getAllSkillNames()) {
                AgentSkill sk = repo.getSkill(name);
                if (sk != null) {
                    box.registerSkill(sk);
                }
            }
            box.registerSkillLoadTool();
            agentBuilder.skillBox(box);
        } catch (Exception e) {
            log.debug("MAG skills skipped: {}", e.toString());
        }
    }

    private static String buildSysPrompt(MagAgent agent, boolean isPm) {
        String base =
                "你是多 Agent 协作平台中的职能 Agent，角色类型："
                        + agent.getRoleType()
                        + "；名称："
                        + (StringUtils.hasText(agent.getName()) ? agent.getName() : ("agent-" + agent.getId()))
                        + "。所有业务动作须通过本对话中的工具完成；会话与数据范围严格限定在当前项目 projectId="
                        + agent.getProjectId()
                        + "。"
                        + "你可使用 invoke_peer_agent 与同项目其他 Agent（Agent2Agent）协作。"
                        + "若环境启用了 MCP，还可调用 MCP 暴露的工具。";
        if (isPm) {
            return base
                    + "你是项目经理：使用 list_dispatchable_agents、dispatch_task 协调产品、开发、测试等职能 Agent；"
                    + "派工前若不清楚执行人 id，必须先调用 list_dispatchable_agents。"
                    + "每次派工后或需要掌握全局时，应调用 list_project_tasks 与 list_project_modules，"
                    + "按任务 state（PENDING/IN_PROGRESS/BLOCKED/DONE）与模块覆盖分析进度与未完项；"
                    + "若仍有明确缺口且可指派，继续 dispatch_task；若当前无合适新增任务、应等待执行方推进或等待各职能主 Agent 通过 mag_ask_pm_for_next_tasks 要活，"
                    + "须在回复中简要说明进度结论与下一步等待点。"
                    + "系统可能在任务结项（DONE）后自动触发你的一轮复盘编排，须按说明用工具检查是否仍有非 DONE 任务并决定是否再派工或声明本阶段已全部完成。"
                    + "需要结构化思考时可先用 pm_delegate_reflection（Agent as Tool）。"
                    + "若用户意图已明确，应调用工具完成派工并给出简短总结；回答须简洁。";
        }
        if ("PRODUCT".equals(agent.getRoleType())) {
            return base
                    + "你是产品职能：先 mag_read_requirement_doc 理解需求，再 mag_submit_dev_requirement_candidate 输出可评审的开发需求说明候选。"
                    + coordinationHierarchySuffix(agent, isPm)
                    + "回答须简洁。";
        }
        if ("FRONTEND".equals(agent.getRoleType()) || "BACKEND".equals(agent.getRoleType())) {
            return base
                    + "你是开发职能（"
                    + agent.getRoleType()
                    + "）：用 mag_record_implementation_plan 分别记录前端或后端实现要点，便于测试编写单测。"
                    + coordinationHierarchySuffix(agent, isPm)
                    + "回答须简洁。";
        }
        if ("TEST".equals(agent.getRoleType())) {
            return base
                    + "你是测试职能：可用 mag_record_unit_test_plan 记录单测范围与关键断言，便于开发与项目经理 visibility。"
                    + coordinationHierarchySuffix(agent, isPm)
                    + "回答须简洁。";
        }
        return base + coordinationHierarchySuffix(agent, isPm) + "回答须简洁。";
    }

    /**
     * 产品 §4.1–§4.2：主 Agent 向项目经理要派工（{@link MagMainAgentPmRequestTools}）；子 Agent 向主 Agent 协作。
     */
    private static String coordinationHierarchySuffix(MagAgent agent, boolean isPm) {
        if (isPm) {
            return "";
        }
        if (agent.getParentAgentId() == null) {
            return " 你是本子线主 Agent（配置中无上级 parent_agent）：当本子线已无活可分、或需要项目经理补充/调整派工与优先级时，"
                    + "应调用 mag_ask_pm_for_next_tasks(situationSummary) 触发项目经理 Agent 编排（对方将使用派工工具）。"
                    + "子 Agent 勿调用该工具，应通过 invoke_peer_agent 联系主 Agent 或协调线程要活。 ";
        }
        return " 你是子 Agent（存在上级 parent_agent）：申领下一项工作时优先使用 invoke_peer_agent 联系主 Agent（instruction 写明当前任务与需要的下一项）；"
                + "勿调用 mag_ask_pm_for_next_tasks。 ";
    }

    private static String buildUserLine(boolean isPm, MagAgent agent, String instruction) {
        if (isPm) {
            String extra =
                    StringUtils.hasText(instruction)
                            ? instruction.trim()
                            : "（未提供额外说明，请根据职责判断是否需要派工。）";
            return "编排已触发（Temporal Activity）。当前项目 projectId="
                    + agent.getProjectId()
                    + "。\n用户/触发方说明："
                    + extra
                    + "\n请按需调用工具：派工前后可用 list_project_tasks、list_project_modules 看进度与未完项；"
                    + "再决定继续 dispatch_task 或说明等待执行方/主 Agent 要活。若无需派工，可只做进度分析并简要回复。";
        }
        String extra =
                StringUtils.hasText(instruction)
                        ? instruction.trim()
                        : "（未提供额外说明，请根据角色与工具完成本轮工作。）";
        return "编排已触发（Temporal Activity）。当前项目 projectId="
                + agent.getProjectId()
                + "。\n说明："
                + extra;
    }

    private static final long MAX_STREAM_BLOCK_SECONDS = 14_400L;

    /**
     * 单次 ReAct 订阅可能包含多轮 LLM + 工具（含同步嵌套 A2A），墙钟远大于单次 HTTP 超时。
     */
    private long resolveStreamBlockTimeoutSeconds(int reactIters) {
        if (streamBlockTimeoutSeconds > 0) {
            return Math.min(streamBlockTimeoutSeconds, MAX_STREAM_BLOCK_SECONDS);
        }
        long per = Math.max(1L, (long) callTimeoutSeconds);
        int it = Math.max(1, reactIters);
        int depthBudget = Math.max(1, Math.min(a2aMaxDepth, 8));
        long estimated = per * it * depthBudget + 180L;
        long floor = per + 30L;
        return Math.min(Math.max(estimated, floor), MAX_STREAM_BLOCK_SECONDS);
    }

    private GenerateOptions generateOptions() {
        return GenerateOptions.builder()
                .executionConfig(
                        ExecutionConfig.builder()
                                .timeout(Duration.ofSeconds(callTimeoutSeconds))
                                .build())
                .build();
    }

    private Model buildChatModel(LlmChannel ch, String apiKey, String model, LlmProtocol protocol) {
        GenerateOptions gen = generateOptions();
        return switch (protocol) {
            case OPENAI_COMPAT -> buildOpenAiCompatible(ch, apiKey, model, gen);
            case ANTHROPIC -> buildAnthropic(ch, apiKey, model, gen);
        };
    }

    private OpenAIChatModel buildOpenAiCompatible(LlmChannel ch, String apiKey, String model, GenerateOptions gen) {
        String path = llmProviderCatalog.resolveCompletionPath(ch.getProviderCode());
        String ep = path.startsWith("/") ? path : "/" + path;
        String base = ch.getBaseUrl() == null ? "" : ch.getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!StringUtils.hasText(base)) {
            throw new IllegalArgumentException("channel base_url required");
        }
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .baseUrl(base)
                .endpointPath(ep)
                .stream(false)
                .generateOptions(gen)
                .build();
    }

    private AnthropicChatModel buildAnthropic(LlmChannel ch, String apiKey, String model, GenerateOptions gen) {
        String base = ch.getBaseUrl() == null ? "" : ch.getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!StringUtils.hasText(base)) {
            throw new IllegalArgumentException("channel base_url required");
        }
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .baseUrl(base)
                .stream(false)
                .defaultOptions(gen)
                .build();
    }

    /**
     * Reactor {@code block()} 可能将 {@link ModelException} 放在包装异常或 cause 链中。
     */
    private static ModelException unwrapModelException(Throwable e) {
        if (e == null) {
            return null;
        }
        Throwable cur = e;
        for (int i = 0; i < 12 && cur != null; i++) {
            if (cur instanceof ModelException me) {
                return me;
            }
            cur = cur.getCause();
        }
        try {
            Throwable unwrapped = Exceptions.unwrap(e);
            if (unwrapped != e) {
                return unwrapModelException(unwrapped);
            }
        } catch (Exception ignored) {
            // unwrap 非 Reactor 异常时可能抛错，忽略
        }
        return null;
    }

    /**
     * 将 AgentScope {@link ModelException} 长文案压缩为便于告警/Temporal 展示的摘要；
     * 若消息中含 {@code | {"error":{...}}} 则解析供应商 {@code error.message}。
     */
    static String summarizeModelException(ModelException e) {
        if (e == null) {
            return "ModelException";
        }
        String msg = e.getMessage();
        if (!StringUtils.hasText(msg)) {
            return e.getClass().getSimpleName();
        }
        int pipe = msg.lastIndexOf(" | ");
        if (pipe < 0 || pipe + 3 >= msg.length()) {
            return msg;
        }
        String head = msg.substring(0, pipe).trim();
        String jsonTail = msg.substring(pipe + 3).trim();
        if (!jsonTail.startsWith("{")) {
            return msg;
        }
        try {
            JsonNode root = MODEL_ERROR_JSON.readTree(jsonTail);
            JsonNode err = root.get("error");
            if (err != null && err.has("message")) {
                String vendorMsg = err.get("message").asText("");
                String code = err.has("code") ? err.get("code").asText("") : "";
                StringBuilder sb = new StringBuilder(head);
                sb.append(" | ");
                if (StringUtils.hasText(vendorMsg)) {
                    sb.append(vendorMsg);
                } else {
                    sb.append(jsonTail);
                }
                if (StringUtils.hasText(code)) {
                    sb.append(" [vendorCode=").append(code).append(']');
                }
                return sb.toString();
            }
        } catch (Exception ignored) {
            // 保留原始消息
        }
        return msg;
    }
}
