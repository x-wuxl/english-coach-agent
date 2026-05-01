package com.wuxl.englishcoach.application.user;

import com.wuxl.englishcoach.api.user.dto.CreateUserProfileRequest;
import com.wuxl.englishcoach.api.user.dto.UserProfileResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.domain.user.UserProfile;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;

    public UserProfileService(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    public UserProfileResponse create(CreateUserProfileRequest request) {
        UserProfileDO userProfileDO = new UserProfileDO();
        userProfileDO.setUserCode(request.userCode());
        userProfileDO.setGoal(request.goal().name());
        userProfileDO.setDailyMinutes(request.dailyMinutes());
        userProfileDO.setStudyStartTime(request.studyStartTime());
        userProfileDO.setReviewTime(request.reviewTime());
        userProfileDO.setStatus("ACTIVE");

        try {
            userProfileMapper.insert(userProfileDO);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCodeEnum.DUPLICATE_USER_CODE);
        }

        return toResponse(toDomain(userProfileDO));
    }

    public UserProfileResponse getById(Long userId) {
        UserProfileDO userProfileDO = userProfileMapper.selectById(userId);
        if (userProfileDO == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        return toResponse(toDomain(userProfileDO));
    }

    private UserProfile toDomain(UserProfileDO userProfileDO) {
        return new UserProfile(
                userProfileDO.getId(),
                userProfileDO.getUserCode(),
                Enum.valueOf(com.wuxl.englishcoach.common.enums.GoalType.class, userProfileDO.getGoal()),
                userProfileDO.getDailyMinutes(),
                userProfileDO.getStudyStartTime(),
                userProfileDO.getReviewTime(),
                userProfileDO.getStatus()
        );
    }

    private UserProfileResponse toResponse(UserProfile userProfile) {
        return new UserProfileResponse(
                userProfile.id(),
                userProfile.userCode(),
                userProfile.goal().name(),
                userProfile.dailyMinutes(),
                userProfile.studyStartTime(),
                userProfile.reviewTime(),
                userProfile.status()
        );
    }
}
