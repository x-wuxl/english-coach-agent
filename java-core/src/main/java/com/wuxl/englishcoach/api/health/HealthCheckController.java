package com.wuxl.englishcoach.api.health;

import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.common.response.BaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @GetMapping
    public BaseResponse<String> health() {
        return BaseResponse.success("ok");
    }

    @GetMapping("/business-error")
    public BaseResponse<Void> businessError() {
        throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
    }

    @PostMapping("/echo")
    public BaseResponse<EchoResponse> echo(@Valid @RequestBody EchoRequest request) {
        return BaseResponse.success(new EchoResponse(request.getName()));
    }

    @Getter
    @Setter
    public static class EchoRequest {
        @NotBlank(message = "must not be blank")
        private String name;
    }

    public record EchoResponse(String name) {
    }
}
