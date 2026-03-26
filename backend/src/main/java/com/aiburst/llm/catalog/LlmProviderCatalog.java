package com.aiburst.llm.catalog;

import com.aiburst.llm.model.LlmProtocol;
import com.aiburst.llm.model.LlmProviderMeta;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LlmProviderCatalog {

    private static final String OAI = "/v1/chat/completions";
    private static final String SUFFIX_V1_CHAT = "/chat/completions";

    private final Map<String, LlmProviderMeta> byCode;

    public LlmProviderCatalog() {
        Map<String, LlmProviderMeta> m = new LinkedHashMap<>();
        add(m, "openai", "OpenAI (ChatGPT)", "https://api.openai.com", OAI, LlmProtocol.OPENAI_COMPAT, "官方 OpenAI API");
        add(m, "azure_openai", "Azure OpenAI", "https://YOUR_RESOURCE.openai.azure.com", OAI, LlmProtocol.OPENAI_COMPAT, "deployment 填在模型名");
        add(m, "deepseek", "DeepSeek", "https://api.deepseek.com", OAI, LlmProtocol.OPENAI_COMPAT, "兼容 OpenAI");
        add(m, "minimax", "MiniMax", "https://api.minimax.chat", OAI, LlmProtocol.OPENAI_COMPAT, "以官方当前兼容地址为准");
        add(m, "glm", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "OpenAI 兼容");
        add(m, "moonshot", "Moonshot (Kimi)", "https://api.moonshot.cn/v1", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "OpenAI 兼容");
        add(m, "baichuan", "百川", "https://api.baichuan-ai.com", OAI, LlmProtocol.OPENAI_COMPAT, "以官方为准");
        add(m, "doubao", "字节豆包 / 火山方舟", "https://ark.cn-beijing.volces.com/api/v3", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "方舟兼容路径以控制台为准");
        add(m, "hunyuan", "腾讯混元", "https://api.hunyuan.cloud.tencent.com", OAI, LlmProtocol.OPENAI_COMPAT, "以官方为准");
        add(m, "qwen_compat", "阿里云通义（兼容模式）", "https://dashscope.aliyuncs.com/compatible-mode/v1", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "DashScope 兼容 OpenAI");
        add(m, "dashscope", "阿里云 DashScope", "https://dashscope.aliyuncs.com/compatible-mode/v1", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "推荐使用兼容 Base");
        add(m, "groq", "Groq", "https://api.groq.com/openai/v1", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "OpenAI 兼容");
        add(m, "ollama", "Ollama（本地）", "http://127.0.0.1:11434/v1", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "本地 OpenAI 兼容");
        add(m, "vllm", "vLLM / 自建网关", "http://127.0.0.1:8000/v1", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "常见 OpenAI 兼容");
        add(m, "cursor_proxy", "Cursor / 工具链代理", "https://your-proxy.example/v1", SUFFIX_V1_CHAT, LlmProtocol.OPENAI_COMPAT, "自建兼容网关，非官方 Cursor 私有 API");
        add(m, "anthropic", "Anthropic Claude", "https://api.anthropic.com", "/v1/messages", LlmProtocol.ANTHROPIC, "Messages API，路径固定 /v1/messages");
        add(m, "gemini", "Google Gemini", "https://generativelanguage.googleapis.com", OAI, LlmProtocol.OPENAI_COMPAT, "若走第三方兼容层请改 Base");
        add(m, "wenxin", "百度千帆 / 文心", "https://qianfan.baidubce.com", OAI, LlmProtocol.OPENAI_COMPAT, "以官方或兼容层为准");
        add(m, "custom_openai", "自定义 OpenAI 兼容", "", OAI, LlmProtocol.OPENAI_COMPAT, "手动填写 Base URL");
        this.byCode = Collections.unmodifiableMap(m);
    }

    private static void add(Map<String, LlmProviderMeta> m, String code, String name, String base, String path,
                            LlmProtocol p, String hint) {
        m.put(code, new LlmProviderMeta(code, name, base, path, p, hint));
    }

    public List<LlmProviderMeta> listAll() {
        return byCode.values().stream().collect(Collectors.toList());
    }

    public LlmProviderMeta find(String code) {
        return code == null ? null : byCode.get(code);
    }

    public LlmProtocol resolveProtocol(String providerCode) {
        LlmProviderMeta meta = find(providerCode);
        if (meta != null) {
            return meta.getProtocol();
        }
        return LlmProtocol.OPENAI_COMPAT;
    }

    public String resolveCompletionPath(String providerCode) {
        LlmProviderMeta meta = find(providerCode);
        if (meta != null && meta.getCompletionPath() != null) {
            return meta.getCompletionPath();
        }
        return OAI;
    }
}
