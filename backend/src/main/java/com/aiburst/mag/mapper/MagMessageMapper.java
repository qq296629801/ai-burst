package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagMessageMapper {

    int insert(MagMessage row);

    List<MagMessage> selectByThreadId(@Param("threadId") Long threadId);
}
