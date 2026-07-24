package com.pyin.plugin.system.user.repository;


import com.pyin.plugin.system.user.entity.UserRoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleRepository extends BaseMapper<UserRoleEntity> {
}
