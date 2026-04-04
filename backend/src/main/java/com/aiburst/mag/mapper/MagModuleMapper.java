package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagModule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagModuleMapper {

    int insert(MagModule row);

    int update(MagModule row);

    int deleteById(@Param("id") Long id);

    MagModule selectById(@Param("id") Long id);

    List<MagModule> selectByProjectId(@Param("projectId") Long projectId);
}
