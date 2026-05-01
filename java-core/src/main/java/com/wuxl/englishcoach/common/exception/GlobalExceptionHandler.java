package com.wuxl.englishcoach.common.exception;

import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.response.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException exception) {
        return BaseResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public BaseResponse<Void> handleValidationException(Exception exception) {
        return BaseResponse.failure(ErrorCodeEnum.INVALID_REQUEST.getCode(), extractMessage(exception));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return BaseResponse.failure(ErrorCodeEnum.INVALID_REQUEST.getCode(), normalizeReadableMessage(exception));
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleGenericException(Exception exception) {
        return BaseResponse.failure(ErrorCodeEnum.INTERNAL_SERVER_ERROR.getCode(), ErrorCodeEnum.INTERNAL_SERVER_ERROR.getMessage());
    }

    private String extractMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        return exception.getMessage();
    }

    private String normalizeReadableMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        return cause != null && cause.getMessage() != null
                ? cause.getMessage()
                : ErrorCodeEnum.INVALID_REQUEST.getMessage();
    }
}
