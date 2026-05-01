package com.wuxl.englishcoach.application.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxl.englishcoach.api.user.dto.CreateUserProfileRequest;
import com.wuxl.englishcoach.api.user.dto.UserProfileResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.domain.user.UserProfile;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;

    public UserProfileService(UserProfileMapper userProfileMapper, ObjectMapper objectMapper) {
        this.userProfileMapper = userProfileMapper;
        this.objectMapper = objectMapper;
    }

    public UserProfileResponse create(CreateUserProfileRequest request) {
        UserProfileDO userProfileDO = new UserProfileDO();
        userProfileDO.setUserCode(request.userCode());
        userProfileDO.setGoal(request.goal().name());
        userProfileDO.setSubGoals(toJson(request.subGoals()));
        userProfileDO.setDailyMinutes(request.dailyMinutes());
        userProfileDO.setStudyStartTime(request.studyStartTime());
        userProfileDO.setReviewTime(request.reviewTime());
        userProfileDO.setPreferredModes(toJson(request.preferredModes()));
        userProfileDO.setMotivationStyle(request.motivationStyle());
        userProfileDO.setFatigueTolerance(request.fatigueTolerance());
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
                com.wuxl.englishcoach.common.enums.GoalType.valueOf(userProfileDO.getGoal()),
                fromJson(userProfileDO.getSubGoals()),
                userProfileDO.getDailyMinutes(),
                userProfileDO.getStudyStartTime(),
                userProfileDO.getReviewTime(),
                userProfileDO.getOverallLevel(),
                userProfileDO.getVocabLevel(),
                userProfileDO.getGrammarLevel(),
                userProfileDO.getReadingLevel(),
                userProfileDO.getOutputLevel(),
                fromJson(userProfileDO.getPreferredModes()),
                userProfileDO.getMotivationStyle(),
                userProfileDO.getFatigueTolerance(),
                userProfileDO.getStatus()
        );
    }

    private UserProfileResponse toResponse(UserProfile userProfile) {
        return new UserProfileResponse(
                userProfile.id(),
                userProfile.userCode(),
                userProfile.goal().name(),
                userProfile.subGoals(),
                userProfile.dailyMinutes(),
                userProfile.studyStartTime(),
                userProfile.reviewTime(),
                userProfile.overallLevel(),
                userProfile.vocabLevel(),
                userProfile.grammarLevel(),
                userProfile.readingLevel(),
                userProfile.outputLevel(),
                userProfile.preferredModes(),
                userProfile.motivationStyle(),
                userProfile.fatigueTolerance(),
                userProfile.status()
        );
    }

    private String toJson(List<String> list) {
        if (list == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
