package com.pyin.center.auth.web;

import cn.dev33.satoken.exception.NotLoginException;
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
}
