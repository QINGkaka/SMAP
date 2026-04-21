package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ApiResponse<Void> handleIoException(IOException exception) {
        return ApiResponse.fail("本地文件读写失败：" + exception.getMessage());
    }
}
