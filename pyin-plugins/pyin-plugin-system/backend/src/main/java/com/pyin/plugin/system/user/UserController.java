package com.pyin.plugin.system.user;

import com.pyin.plugin.common.api.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<?> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String status
    ) {
        return Result.ok(userService.findAll(new UserQuery(username, displayName, status)));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        UserDetail detail = userService.findDetail(id);
        if (detail == null) {
            return Result.fail("PYIN-USER-404", "用户不存在: " + id);
        }
        return Result.ok(detail);
    }

    @PostMapping
    public Result<?> create(@RequestBody(required = false) CreateUserRequest request) {
        try {
            UserEntity entity = userService.create(request);
            return Result.ok(userService.findDetail(entity.getId()));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-USER-400", exception.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody(required = false) UpdateUserRequest request) {
        try {
            userService.update(id, request);
            return Result.ok(userService.findDetail(id));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-USER-400", exception.getMessage());
        }
    }

    @PostMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody(required = false) ResetUserPasswordRequest request) {
        try {
            userService.resetPassword(id, request);
            return Result.ok();
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-USER-400", exception.getMessage());
        }
    }

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
