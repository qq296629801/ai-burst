package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagExternalFetchAudit;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagExternalFetchAuditMapper {

    int insert(MagExternalFetchAudit row);

    List<MagExternalFetchAudit> selectByProjectId(@Param("projectId") Long projectId);
}
