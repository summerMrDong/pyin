package com.pyin.plugin.system.setting.repository;


import com.pyin.plugin.system.setting.entity.SystemSettingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemSettingRepository extends BaseMapper<SystemSettingEntity> {
}
