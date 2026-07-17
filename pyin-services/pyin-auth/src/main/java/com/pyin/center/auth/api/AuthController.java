package com.pyin.center.auth.api;

import cn.dev33.satoken.stp.StpUtil;
import com.pyin.center.auth.admin.AdminAuthService;
import com.pyin.center.auth.admin.AdminUserIdentity;
import com.pyin.plugin.common.api.Result;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminAuthService adminAuthService;

    public AuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<Result<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.badRequest().body(Result.fail("PYIN-AUTH-400", "账号和密码不能为空"));
        }

        AdminUserIdentity user = adminAuthService.authenticate(request.getUsername().trim(), request.getPassword());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.fail("PYIN-AUTH-401", "账号或密码错误"));
        }

        StpUtil.login(user.id());
        return ResponseEntity.ok(Result.ok(Map.of(
                "token", StpUtil.getTokenValue(),
                "loginId", StpUtil.getLoginIdAsLong()
        )));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @GetMapping("/current-user")
    public Result<Map<String, Object>> currentUser() {
        Long loginId = StpUtil.getLoginIdAsLong();
        AdminUserIdentity user = adminAuthService.currentUser(loginId);
        return Result.ok(Map.of(
                "loginId", loginId,
                "username", user.username(),
                "displayName", user.displayName()
        ));
    }
}
