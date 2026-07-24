package com.pyin.plugin.system.plugin.repository;


import com.pyin.plugin.system.plugin.entity.PluginEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PluginRepository extends BaseMapper<PluginEntity> {
}
