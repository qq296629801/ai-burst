package com.aiburst.rbac.mapper;

import com.aiburst.rbac.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    SysUser selectByUsername(@Param("username") String username);

    SysUser selectById(@Param("id") Long id);

    int countByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);

    int insert(SysUser user);

    int update(SysUser user);

    int deleteById(@Param("id") Long id);

    List<SysUser> selectList(@Param("username") String username, @Param("status") Integer status);

    void deleteUserRoles(@Param("userId") Long userId);

    void deleteUsersByRoleId(@Param("roleId") Long roleId);

    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    List<Long> selectAllIds();
}
