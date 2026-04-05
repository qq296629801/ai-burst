package com.aiburst.rbac.mapper;

import com.aiburst.rbac.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMapper {

    List<SysRole> selectAll();

    SysRole selectById(@Param("id") Long id);

    int countByCode(@Param("roleCode") String roleCode, @Param("excludeId") Long excludeId);

    int insert(SysRole role);

    int update(SysRole role);

    int deleteById(@Param("id") Long id);

    void deleteRolePermissions(@Param("roleId") Long roleId);

    void deletePermissionLinksByPermId(@Param("permId") Long permId);

    void insertRolePermission(@Param("roleId") Long roleId, @Param("permId") Long permId);

    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
