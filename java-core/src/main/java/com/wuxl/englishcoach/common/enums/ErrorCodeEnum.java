package com.wuxl.englishcoach.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {

    SUCCESS(0, "ok"),
    INVALID_REQUEST(4001, "invalid request"),
    INVALID_ENUM_VALUE(4002, "invalid enum value"),
    INVALID_DATE_TIME_FORMAT(4003, "invalid date/time format"),
    INVALID_PAGE_PARAMETER(4004, "invalid page parameter"),
    UNAUTHORIZED(4010, "unauthorized"),
    FORBIDDEN(4030, "forbidden"),
    USER_NOT_FOUND(4041, "user not found"),
    STUDY_SESSION_NOT_FOUND(4042, "study session not found"),
    DAILY_PLAN_NOT_FOUND(4043, "daily plan not found"),
    LEARNING_ITEM_NOT_FOUND(4044, "learning item not found"),
    WEEKLY_REVIEW_NOT_FOUND(4045, "weekly review not found"),
    DUPLICATE_USER_CODE(4091, "user code already exists"),
    DUPLICATE_DAILY_PLAN(4092, "daily plan already exists"),
    SESSION_ALREADY_COMPLETED(4093, "session already completed"),
    EMPTY_PLACEMENT_SUBMISSION(4094, "placement submission cannot be empty"),
    DOMAIN_LOGIC_ERROR(5001, "internal domain logic error"),
    PERSISTENCE_ERROR(5002, "data persistence error"),
    INTERNAL_SERVER_ERROR(5000, "internal server error");

    private final Integer code;
    private final String message;

    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
