package com.aiburst.mag.config;

import com.aiburst.mag.temporal.MagAgentRunWorkflowImpl;
import com.aiburst.mag.temporal.MagOrchestrationActivitiesImpl;
import com.aiburst.mag.temporal.MagThreadRunWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 与 Spring Boot 同进程启动 Temporal Worker（开发/单机）；生产可拆独立进程，队列名与配置一致即可。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aiburst.mag.temporal", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class MagTemporalWorkerBootstrap {

    private final WorkerFactory workerFactory;
    private final MagTemporalProperties properties;
    private final MagOrchestrationActivitiesImpl orchestrationActivities;

    @PostConstruct
    void startWorker() {
        Worker worker = workerFactory.newWorker(properties.getTaskQueue());
        worker.registerWorkflowImplementationTypes(MagAgentRunWorkflowImpl.class, MagThreadRunWorkflowImpl.class);
        worker.registerActivitiesImplementations(orchestrationActivities);
        workerFactory.start();
        log.info(
                "MAG Temporal Worker started, taskQueue={} namespace={} (Activity StartToClose 由启动 Workflow 时传入，配置"
                        + " aiburst.mag.temporal.activity-start-to-close-minutes 默认 {} 分钟)",
                properties.getTaskQueue(),
                properties.getNamespace(),
                properties.getEffectiveActivityStartToCloseMinutes());
    }
}
