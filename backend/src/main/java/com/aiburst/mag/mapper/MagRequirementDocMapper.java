package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagRequirementDoc;
import org.apache.ibatis.annotations.Param;

public interface MagRequirementDocMapper {

    int insert(MagRequirementDoc row);

    int updateCurrentVersion(@Param("id") Long id, @Param("version") int version);

    MagRequirementDoc selectByProjectId(@Param("projectId") Long projectId);

    MagRequirementDoc selectById(@Param("id") Long id);
}
