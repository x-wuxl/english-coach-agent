package com.wuxl.englishcoach.common.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseResponse<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, "ok", data);
    }

    public static BaseResponse<Void> success() {
        return new BaseResponse<>(0, "ok", null);
    }

    public static <T> BaseResponse<T> failure(Integer code, String message) {
        return new BaseResponse<>(code, message, null);
    }
}
