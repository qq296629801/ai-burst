package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagAgent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagAgentMapper {

    int insert(MagAgent row);

    int update(MagAgent row);

    MagAgent selectById(@Param("id") Long id);

    List<MagAgent> selectByProjectId(@Param("projectId") Long projectId);

    int countByProjectId(@Param("projectId") Long projectId);
}
