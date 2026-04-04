package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagReleaseArchive;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagReleaseArchiveMapper {

    int insert(MagReleaseArchive row);

    MagReleaseArchive selectById(@Param("id") Long id);

    List<MagReleaseArchive> selectByProjectId(@Param("projectId") Long projectId);
}
