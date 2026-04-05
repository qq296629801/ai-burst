package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagThread;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MagThreadMapper {

    int insert(MagThread row);

    MagThread selectById(@Param("id") Long id);

    /** 任务专属沟通线程（项目经理派工时创建），按 id 倒序取最新一条 */
    MagThread selectLatestByTaskId(@Param("taskId") Long taskId);

    List<MagThread> selectByProjectId(@Param("projectId") Long projectId);

    LocalDateTime selectLatestMessageAtByProjectId(@Param("projectId") Long projectId);
}
