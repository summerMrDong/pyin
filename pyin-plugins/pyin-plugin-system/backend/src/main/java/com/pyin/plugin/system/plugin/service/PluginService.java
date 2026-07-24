package com.pyin.plugin.system.plugin.service;


import com.pyin.plugin.system.plugin.entity.PluginEntity;
import java.util.List;

public interface PluginService {

    List<PluginEntity> findAll();
}
