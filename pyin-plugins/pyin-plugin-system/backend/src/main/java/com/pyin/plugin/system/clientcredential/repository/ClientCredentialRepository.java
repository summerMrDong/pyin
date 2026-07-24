package com.pyin.plugin.system.clientcredential.repository;


import com.pyin.plugin.system.clientcredential.entity.ClientCredentialEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClientCredentialRepository extends BaseMapper<ClientCredentialEntity> {
}
