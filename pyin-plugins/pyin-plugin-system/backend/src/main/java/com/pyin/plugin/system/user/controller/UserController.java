package com.pyin.plugin.system.user.controller;


import com.pyin.plugin.system.user.entity.UserEntity;
import com.pyin.plugin.system.user.model.CreateUserRequest;
import com.pyin.plugin.system.user.model.ResetUserPasswordRequest;
import com.pyin.plugin.system.user.model.UpdateUserRequest;
import com.pyin.plugin.system.user.model.UserDetail;
import com.pyin.plugin.system.user.model.UserQuery;
import com.pyin.plugin.system.user.service.UserService;
import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@AdminMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Permission(code = "user:view", name = "用户查看")
    @GetMapping
    public Result<?> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String status
    ) {
        return Result.ok(userService.findAll(new UserQuery(username, displayName, status)));
    }

    @Permission(code = "user:view", name = "用户查看")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        UserDetail detail = userService.findDetail(id);
        if (detail == null) {
            return Result.fail("PYIN-USER-404", "用户不存在: " + id);
        }
        return Result.ok(detail);
    }

    @Permission(code = "user:create", name = "用户创建")
    @PostMapping
    public Result<?> create(@RequestBody(required = false) CreateUserRequest request) {
        try {
            UserEntity entity = userService.create(request);
            return Result.ok(userService.findDetail(entity.getId()));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-USER-400", exception.getMessage());
        }
    }

    @Permission(code = "user:update", name = "用户更新")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody(required = false) UpdateUserRequest request) {
        try {
            userService.update(id, request);
            return Result.ok(userService.findDetail(id));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-USER-400", exception.getMessage());
        }
    }

    @Permission(code = "user:reset-password", name = "用户重置密码")
    @PostMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody(required = false) ResetUserPasswordRequest request) {
        try {
            userService.resetPassword(id, request);
            return Result.ok();
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-USER-400", exception.getMessage());
        }
    }

    @Permission(code = "user:delete", name = "用户删除")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            userService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-USER-404", exception.getMessage());
        } catch (IllegalStateException exception) {
            return Result.fail("PYIN-USER-409", exception.getMessage());
        }
    }
}
