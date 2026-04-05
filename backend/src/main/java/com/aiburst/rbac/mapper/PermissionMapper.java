package com.aiburst.rbac.mapper;

import com.aiburst.rbac.entity.SysPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PermissionMapper {

    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    List<SysPermission> selectMenuByUserId(@Param("userId") Long userId);

    List<SysPermission> selectAllOrderBySort();

    SysPermission selectById(@Param("id") Long id);

    int countByCode(@Param("permCode") String permCode, @Param("excludeId") Long excludeId);

    int insert(SysPermission p);

    int update(SysPermission p);

    int deleteById(@Param("id") Long id);

    int countChildren(@Param("parentId") Long parentId);
}
