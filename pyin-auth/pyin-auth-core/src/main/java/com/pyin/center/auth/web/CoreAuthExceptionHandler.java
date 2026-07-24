package com.pyin.center.auth.web;

import cn.dev33.satoken.exception.NotLoginException;
import com.pyin.center.auth.authentication.AuthenticationException;
import com.pyin.center.auth.authentication.AuthorizationException;
import com.pyin.plugin.common.api.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CoreAuthExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLogin(NotLoginException exception) {
        return Result.fail("PYIN-AUTH-401", "Login required");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthentication(AuthenticationException exception) {
        return Result.fail("PYIN-AUTH-401", exception.getMessage());
    }

    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAuthorization(AuthorizationException exception) {
        return Result.fail("PYIN-AUTH-403", exception.getMessage());
    }
}
