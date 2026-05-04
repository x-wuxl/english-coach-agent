package com.wuxl.englishcoach.api.user;

import com.wuxl.englishcoach.api.user.dto.CreateUserProfileRequest;
import com.wuxl.englishcoach.api.user.dto.UserProfileResponse;
import com.wuxl.englishcoach.application.user.UserProfileService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping
    public BaseResponse<UserProfileResponse> create(@Valid @RequestBody CreateUserProfileRequest request) {
        return BaseResponse.success(userProfileService.create(request));
    }

    @GetMapping("/{userId}")
    public BaseResponse<UserProfileResponse> getById(@PathVariable Long userId) {
        return BaseResponse.success(userProfileService.getById(userId));
    }

    @GetMapping("/by-code/{userCode}")
    public BaseResponse<UserProfileResponse> getByUserCode(@PathVariable String userCode) {
        return BaseResponse.success(userProfileService.getByUserCode(userCode));
    }
}
