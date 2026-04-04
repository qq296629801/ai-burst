package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagProject;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagProjectMapper {

    int insert(MagProject row);

    int update(MagProject row);

    int updateCurrentReqDocId(@Param("id") Long id, @Param("docId") Long docId);

    MagProject selectById(@Param("id") Long id);

    List<MagProject> selectByUserId(@Param("userId") Long userId);
}
