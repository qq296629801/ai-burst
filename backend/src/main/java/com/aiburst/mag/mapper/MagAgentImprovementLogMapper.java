package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagAgentImprovementLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MagAgentImprovementLogMapper {

    int insert(MagAgentImprovementLog row);

    List<MagAgentImprovementLog> selectByProjectAndAgent(@Param("projectId") Long projectId,
                                                         @Param("agentId") Long agentId);

    List<MagAgentImprovementLog> selectByProjectId(
            @Param("projectId") Long projectId, @Param("limit") int limit);

    /**
     * 统计某 Agent 在指定时间之后写入的改进日志条数（用于判定「本次编排是否落库产出物」）。
     */
    int countByProjectAgentCreatedAtSince(
            @Param("projectId") long projectId,
            @Param("agentId") long agentId,
            @Param("sinceInclusive") LocalDateTime sinceInclusive);
}
