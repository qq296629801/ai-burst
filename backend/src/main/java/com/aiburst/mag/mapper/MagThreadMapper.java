package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagThread;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MagThreadMapper {

    int insert(MagThread row);

    MagThread selectById(@Param("id") Long id);

    List<MagThread> selectByProjectId(@Param("projectId") Long projectId);

    LocalDateTime selectLatestMessageAtByProjectId(@Param("projectId") Long projectId);
}
