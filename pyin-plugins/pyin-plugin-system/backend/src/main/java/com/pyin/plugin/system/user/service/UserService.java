package com.pyin.plugin.system.user.service;


import com.pyin.plugin.system.user.entity.UserEntity;
import com.pyin.plugin.system.user.model.CreateUserRequest;
import com.pyin.plugin.system.user.model.ResetUserPasswordRequest;
import com.pyin.plugin.system.user.model.UpdateUserRequest;
import com.pyin.plugin.system.user.model.UserDetail;
import com.pyin.plugin.system.user.model.UserQuery;
import com.pyin.plugin.system.user.model.UserSummary;
import java.util.List;

public interface UserService {

    String STATUS_ENABLED = "ENABLED";

    String STATUS_DISABLED = "DISABLED";

    List<UserSummary> findAll(UserQuery query);

    UserDetail findDetail(Long id);

    UserEntity findById(Long id);

    UserEntity findByUsername(String username);

    UserEntity create(CreateUserRequest request);

    UserEntity update(Long id, UpdateUserRequest request);

    void resetPassword(Long id, ResetUserPasswordRequest request);

    void delete(Long id);
}
