package com.aiburst.mag.mapper;

import com.aiburst.mag.entity.MagKbEntry;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MagKbEntryMapper {

    int insert(MagKbEntry row);

    int update(MagKbEntry row);

    int deleteById(@Param("id") Long id);

    MagKbEntry selectById(@Param("id") Long id);

    List<MagKbEntry> selectAll(@Param("keyword") String keyword);
}
