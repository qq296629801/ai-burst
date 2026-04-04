package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagAlertEvent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagAlertEventMapper {

    int insert(MagAlertEvent row);

    List<MagAlertEvent> selectByProjectId(@Param("projectId") Long projectId);

    MagAlertEvent selectById(@Param("id") Long id);

    int updateAcknowledged(@Param("id") Long id, @Param("acknowledged") int acknowledged);
}
