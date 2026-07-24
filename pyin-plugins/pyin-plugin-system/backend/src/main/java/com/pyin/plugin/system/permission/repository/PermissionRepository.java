package com.pyin.plugin.system.permission.repository;


import com.pyin.plugin.system.permission.entity.PermissionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionRepository extends BaseMapper<PermissionEntity> {
}
