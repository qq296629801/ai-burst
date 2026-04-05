package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagTaskExecutionLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagTaskExecutionLogMapper {

    int insert(MagTaskExecutionLog row);

    List<MagTaskExecutionLog> selectByTaskId(@Param("taskId") Long taskId);
}
