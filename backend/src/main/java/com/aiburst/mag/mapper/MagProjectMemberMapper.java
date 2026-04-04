package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagProjectMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagProjectMemberMapper {

    int insert(MagProjectMember row);

    int deleteByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    List<MagProjectMember> selectByProjectId(@Param("projectId") Long projectId);

    String selectRole(@Param("projectId") Long projectId, @Param("userId") Long userId);

    int countMember(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
