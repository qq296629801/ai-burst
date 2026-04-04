package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagAgentImprovementLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagAgentImprovementLogMapper {

    int insert(MagAgentImprovementLog row);

    List<MagAgentImprovementLog> selectByProjectAndAgent(@Param("projectId") Long projectId,
                                                         @Param("agentId") Long agentId);
}
