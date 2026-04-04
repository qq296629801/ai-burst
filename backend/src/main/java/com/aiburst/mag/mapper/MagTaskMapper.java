package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MagTaskMapper {

    int insert(MagTask row);

    int update(MagTask row);

    int updateStateWithVersion(@Param("id") Long id,
                               @Param("state") String state,
                               @Param("expectedVersion") int expectedVersion,
                               @Param("temporalWorkflowId") String temporalWorkflowId);

    MagTask selectById(@Param("id") Long id);

    List<MagTask> selectByProjectId(@Param("projectId") Long projectId);

    List<Map<String, Object>> countByState(@Param("projectId") Long projectId);
}
