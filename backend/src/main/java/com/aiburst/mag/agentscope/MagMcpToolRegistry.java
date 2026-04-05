package com.aiburst.mag.agentscope;

import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.core.tool.mcp.McpTool;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 可选 MCP：将远端 MCP Server 暴露的工具注册进 AgentScope {@link Toolkit}，由大模型调度调用。
 * 连接在首次需要时建立，随 Spring 容器销毁关闭。
 */
@Slf4j
@Service
public class MagMcpToolRegistry implements DisposableBean {

    @Value("${aiburst.mag.mcp.enabled:false}")
    private boolean enabled;

    @Value("${aiburst.mag.mcp.sse-url:}")
    private String sseUrl;

    private final Object lock = new Object();
    private volatile McpClientWrapper client;
    private volatile boolean initFailed;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 将 MCP 工具并入当前 Agent 的 Toolkit（与 PM/产品/开发等工具共存）。
     */
    public void registerInto(Toolkit toolkit) {
        if (!enabled || !StringUtils.hasText(sseUrl)) {
            return;
        }
        if (initFailed || closed.get()) {
            return;
        }
        synchronized (lock) {
            if (closed.get()) {
                return;
            }
            if (client == null && !initFailed) {
                try {
                    McpClientWrapper w =
                            McpClientBuilder.create("mag-mcp")
                                    .sseTransport(sseUrl.trim())
                                    .timeout(Duration.ofSeconds(45))
                                    .buildSync();
                    w.initialize().block(Duration.ofSeconds(60));
                    this.client = w;
                    log.info("MAG MCP connected sseUrl={}", sseUrl.trim());
                } catch (Exception e) {
                    initFailed = true;
                    log.warn("MAG MCP init failed: {}", e.toString());
                    return;
                }
            }
            if (client == null) {
                return;
            }
            try {
                var tools =
                        client.listTools()
                                .blockOptional(Duration.ofSeconds(60))
                                .orElse(Collections.emptyList());
                for (McpSchema.Tool t : tools) {
                    McpSchema.JsonSchema schema = t.inputSchema();
                    Map<String, Object> params =
                            schema != null
                                    ? McpTool.convertMcpSchemaToParameters(schema, Set.of())
                                    : Map.of();
                    String desc = t.description() != null ? t.description() : "";
                    toolkit.registerTool(
                            new McpTool(t.name(), desc, params, client, Map.of()));
                }
                log.info("MAG MCP registered tool count={}", tools.size());
            } catch (Exception e) {
                log.warn("MAG MCP listTools/register failed: {}", e.toString());
            }
        }
    }

    @Override
    public void destroy() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (lock) {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.debug("mcp close: {}", e.toString());
                }
                client = null;
            }
        }
    }
}
