package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagOrchestrationRun;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagOrchestrationRunMapper {

    int insert(MagOrchestrationRun row);

    MagOrchestrationRun selectById(@Param("id") Long id);

    MagOrchestrationRun selectByWorkflowId(@Param("workflowId") String workflowId);

    int updateRunning(@Param("workflowId") String workflowId);

    int updateFinished(
            @Param("workflowId") String workflowId,
            @Param("status") String status,
            @Param("resultSummary") String resultSummary,
            @Param("finishedAt") java.time.LocalDateTime finishedAt);

    List<MagOrchestrationRun> selectByProjectId(
            @Param("projectId") Long projectId, @Param("limit") int limit);
}
