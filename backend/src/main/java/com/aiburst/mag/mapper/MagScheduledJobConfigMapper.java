package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagScheduledJobConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagScheduledJobConfigMapper {

    int insert(MagScheduledJobConfig row);

    int update(MagScheduledJobConfig row);

    MagScheduledJobConfig selectById(@Param("id") Long id);

    List<MagScheduledJobConfig> selectAll();

    List<MagScheduledJobConfig> selectByProjectId(@Param("projectId") Long projectId);

    MagScheduledJobConfig selectByJobKey(@Param("jobKey") String jobKey);
}
