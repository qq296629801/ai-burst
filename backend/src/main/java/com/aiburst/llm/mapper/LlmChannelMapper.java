package com.aiburst.llm.mapper;

import com.aiburst.llm.entity.LlmChannel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LlmChannelMapper {

    List<LlmChannel> selectByOwner(@Param("ownerUserId") Long ownerUserId);

    LlmChannel selectByIdAndOwner(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    int insert(LlmChannel row);

    int update(LlmChannel row);

    int deleteByIdAndOwner(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);
}
