package com.pyin.plugin.system.clientcredential.repository;


import com.pyin.plugin.system.clientcredential.entity.ClientRequestLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClientRequestLogRepository extends BaseMapper<ClientRequestLogEntity> {
}
