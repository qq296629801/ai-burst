package com.aiburst.mag.agentscope;

import com.aiburst.llm.catalog.LlmProviderCatalog;
import com.aiburst.llm.crypto.LlmCryptoService;
import com.aiburst.llm.entity.LlmChannel;
import com.aiburst.llm.mapper.LlmChannelMapper;
import com.aiburst.llm.model.LlmProtocol;
import com.aiburst.mag.MagBusinessException;
import com.aiburst.mag.MagResultCode;
import com.aiburst.mag.entity.MagAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

/**
 * 在 Temporal Activity 内使用 AgentScope Java 调用 Agent 绑定的 {@link LlmChannel}（OpenAI 兼容 / Anthropic）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MagAgentScopeRunService {

    private final LlmChannelMapper llmChannelMapper;
    private final LlmCryptoService llmCryptoService;
    private final LlmProviderCatalog llmProviderCatalog;

    @Value("${aiburst.mag.agentscope.max-iters:4}")
    private int maxIters;

    @Value("${aiburst.mag.agentscope.call-timeout-seconds:120}")
    private int callTimeoutSeconds;

    /**
     * 执行一次轻量对话回合，用于验证通道与编排链路；返回模型文本（可能为空则调用方记为 OK）。
     */
    public String executeAgentRun(MagAgent agent, long triggerUserId) {
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

        String sysPrompt =
                "你是多 Agent 协作平台中的职能 Agent，角色类型："
                        + agent.getRoleType()
                        + "；名称："
                        + (StringUtils.hasText(agent.getName()) ? agent.getName() : ("agent-" + agent.getId()))
                        + "。回答须简洁。";

        ReActAgent reactAgent =
                ReActAgent.builder()
                        .name("mag-" + agent.getId())
                        .sysPrompt(sysPrompt)
                        .model(chatModel)
                        .generateOptions(gen)
                        .maxIters(maxIters)
                        .enableMetaTool(false)
                        .build();

        String userLine =
                "编排已触发（Temporal Activity）。请用一两句话确认在线，并说明当前可承接的工作类型（与角色一致即可）。";

        Msg reply =
                reactAgent
                        .call(List.of(Msg.builder().role(MsgRole.USER).textContent(userLine).build()))
                        .block(Duration.ofSeconds((long) callTimeoutSeconds + 30L));

        String text =
                reply != null && StringUtils.hasText(reply.getTextContent())
                        ? reply.getTextContent().trim()
                        : "";
        log.info("MAG AgentScope agentId={} replyChars={}", agent.getId(), text.length());
        return text.isEmpty() ? "OK" : text;
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
}
