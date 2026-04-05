package com.aiburst.mag.agentscope;

import com.aiburst.mag.entity.MagAgent;

/**
 * Agent2Agent：由工具回调触发同项目另一 {@link MagAgent} 的嵌套编排（同步 ReAct），
 * 实现由 {@link MagAgentScopeRunService} 提供。
 */
@FunctionalInterface
public interface MagNestedAgentRunner {

    String run(MagAgent peer, long triggerUserId, String instruction);
}
