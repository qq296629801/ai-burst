package com.aiburst.mag.agentscope;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 记录 {@link MagPeerInvokeTools} 发起嵌套 {@code executeAgentRun} 时的调用方 Agent id，
 * 供 {@link MagAgentScopeRunService#executeAgentRunWithinDepth} 写入协调线程消息。
 */
public final class MagA2aCallerStack {

    private static final ThreadLocal<Deque<Long>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private MagA2aCallerStack() {}

    public static void push(long callerAgentId) {
        STACK.get().addFirst(callerAgentId);
    }

    public static void pop() {
        Deque<Long> d = STACK.get();
        if (!d.isEmpty()) {
            d.removeFirst();
        }
    }

    /** 当前嵌套层对应的调用方；无则 null */
    public static Long peek() {
        Deque<Long> d = STACK.get();
        return d.isEmpty() ? null : d.peekFirst();
    }
}
