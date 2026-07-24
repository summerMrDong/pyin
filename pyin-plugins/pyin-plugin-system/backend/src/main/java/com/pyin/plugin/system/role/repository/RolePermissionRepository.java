package com.pyin.plugin.system.role.repository;


import com.pyin.plugin.system.role.entity.RolePermissionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionRepository extends BaseMapper<RolePermissionEntity> {
}
