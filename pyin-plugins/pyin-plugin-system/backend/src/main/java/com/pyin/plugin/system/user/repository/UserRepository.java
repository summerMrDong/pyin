package com.pyin.plugin.system.user.repository;


import com.pyin.plugin.system.user.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRepository extends BaseMapper<UserEntity> {
}
