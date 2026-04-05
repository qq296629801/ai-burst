package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagTaskFlowEvent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagTaskFlowEventMapper {

    int insert(MagTaskFlowEvent row);

    List<MagTaskFlowEvent> selectByTaskId(@Param("taskId") Long taskId);
}
