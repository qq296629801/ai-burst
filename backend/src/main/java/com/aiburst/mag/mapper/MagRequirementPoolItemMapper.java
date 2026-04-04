package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagRequirementPoolItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagRequirementPoolItemMapper {

    int insert(MagRequirementPoolItem row);

    int update(MagRequirementPoolItem row);

    MagRequirementPoolItem selectById(@Param("id") Long id);

    List<MagRequirementPoolItem> selectByProjectId(@Param("projectId") Long projectId);

    List<MagRequirementPoolItem> selectTodosForUser(@Param("userId") Long userId,
                                                     @Param("pendingState") String pendingState);
}
