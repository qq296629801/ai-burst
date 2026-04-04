package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagTaskVerification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagTaskVerificationMapper {

    int insert(MagTaskVerification row);

    List<MagTaskVerification> selectByTaskId(@Param("taskId") Long taskId);
}
