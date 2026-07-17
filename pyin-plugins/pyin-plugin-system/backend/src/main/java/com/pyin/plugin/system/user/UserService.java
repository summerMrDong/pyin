package com.pyin.plugin.system.user;

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
