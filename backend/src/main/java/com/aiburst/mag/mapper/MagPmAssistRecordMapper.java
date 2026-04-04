package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagPmAssistRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagPmAssistRecordMapper {

    int insert(MagPmAssistRecord row);

    List<MagPmAssistRecord> selectByProjectId(@Param("projectId") Long projectId);
}
