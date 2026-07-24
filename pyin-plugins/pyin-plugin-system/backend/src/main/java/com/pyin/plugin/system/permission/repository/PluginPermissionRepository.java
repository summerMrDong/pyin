package com.pyin.plugin.system.permission.repository;


import com.pyin.plugin.system.permission.entity.PluginPermissionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PluginPermissionRepository extends BaseMapper<PluginPermissionEntity> {
}
