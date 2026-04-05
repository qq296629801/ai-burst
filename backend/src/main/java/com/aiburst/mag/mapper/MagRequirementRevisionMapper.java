package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagRequirementRevision;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagRequirementRevisionMapper {

    int insert(MagRequirementRevision row);

    MagRequirementRevision selectLatest(@Param("docId") Long docId);

    MagRequirementRevision selectByDocAndVersion(@Param("docId") Long docId, @Param("version") int version);

    MagRequirementRevision selectById(@Param("id") Long id);

    List<MagRequirementRevision> listByDocId(@Param("docId") Long docId);
}
