package com.aiburst.mag.agentscope;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 单工作线程 + 无界队列：根级 AgentScope 编排按提交顺序 <strong>FIFO</strong> 依次执行（先入先出）。
 * 与「栈」不同，任务按排队顺序公平执行；嵌套 A2A 不在此排队，见 {@link MagAgentScopeRunService}。
 */
@Slf4j
@Component
public class MagAgentScopeSerialRunner {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "mag-agent-scope-serial");
                        t.setDaemon(true);
                        return t;
                    });

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        log.info("MAG AgentScope serial executor stopped");
    }
}
