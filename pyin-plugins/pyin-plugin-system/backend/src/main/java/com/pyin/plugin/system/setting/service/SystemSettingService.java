package com.pyin.plugin.system.setting.service;


import com.pyin.plugin.system.setting.entity.SystemSettingEntity;
import java.util.List;

public interface SystemSettingService {

    List<SystemSettingEntity> findAll();
}
