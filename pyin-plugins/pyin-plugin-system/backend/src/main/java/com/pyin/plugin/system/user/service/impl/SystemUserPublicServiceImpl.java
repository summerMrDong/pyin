package com.pyin.plugin.system.user.service.impl;

import com.pyin.plugin.system.api.model.SystemUserAuthInfo;
import com.pyin.plugin.system.api.service.SystemUserPublicService;
import com.pyin.plugin.system.api.model.SystemUserView;
import com.pyin.plugin.system.user.entity.UserEntity;
import com.pyin.plugin.system.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class SystemUserPublicServiceImpl implements SystemUserPublicService {

    private final UserService userService;

    public SystemUserPublicServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public SystemUserAuthInfo findAuthInfoByUsername(String username) {
        return toAuthInfo(userService.findByUsername(username));
    }

    @Override
    public SystemUserAuthInfo findAuthInfoById(Long userId) {
        return toAuthInfo(userService.findById(userId));
    }

    @Override
    public SystemUserView findUserById(Long userId) {
        return toView(userService.findById(userId));
    }

    private SystemUserAuthInfo toAuthInfo(UserEntity user) {
        if (user == null) {
            return null;
        }
        return new SystemUserAuthInfo(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPasswordHash(),
                UserService.STATUS_ENABLED.equals(user.getStatus())
        );
    }

    private SystemUserView toView(UserEntity user) {
        if (user == null) {
            return null;
        }
        return new SystemUserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                UserService.STATUS_ENABLED.equals(user.getStatus())
        );
    }
}
