package com.wuxl.englishcoach.application.mastery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wuxl.englishcoach.api.mastery.dto.MasteryQueryResponse;
import com.wuxl.englishcoach.api.mastery.dto.MasteryStateResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemDO;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemMapper;
import com.wuxl.englishcoach.infrastructure.persistence.mastery.MasteryStateDO;
import com.wuxl.englishcoach.infrastructure.persistence.mastery.MasteryStateMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MasteryStateService {

    private final MasteryStateMapper masteryStateMapper;
    private final LearningItemMapper learningItemMapper;
    private final UserProfileMapper userProfileMapper;

    public MasteryStateService(MasteryStateMapper masteryStateMapper,
                               LearningItemMapper learningItemMapper,
                               UserProfileMapper userProfileMapper) {
        this.masteryStateMapper = masteryStateMapper;
        this.learningItemMapper = learningItemMapper;
        this.userProfileMapper = userProfileMapper;
    }

    public MasteryQueryResponse queryMastery(Long userId, String status, String theme) {
        UserProfileDO user = userProfileMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        LambdaQueryWrapper<MasteryStateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MasteryStateDO::getUserId, userId);
        if (status != null) {
            wrapper.eq(MasteryStateDO::getStatus, status);
        }
        wrapper.orderByDesc(MasteryStateDO::getUpdatedAt);

        List<MasteryStateDO> states = masteryStateMapper.selectList(wrapper);
        List<MasteryStateResponse> responses = new ArrayList<>();

        for (MasteryStateDO ms : states) {
            LearningItemDO item = learningItemMapper.selectById(ms.getLearningItemId());
            if (item == null) continue;
            if (theme != null && !theme.equals(item.getTheme())) continue;

            responses.add(new MasteryStateResponse(
                    ms.getLearningItemId(), item.getContent(),
                    ms.getSeenCount(), ms.getCorrectCount(), ms.getWrongCount(),
                    ms.getCorrectStreak(), ms.getRecognitionScore(), ms.getOutputScore(),
                    ms.getForgetRisk(), ms.getStatus(), ms.getRecommendedMode(),
                    ms.getLastSeenAt(), ms.getNextReviewAt()));
        }

        return new MasteryQueryResponse(responses, responses.size());
    }

    public MasteryQueryResponse getDueReview(Long userId) {
        UserProfileDO user = userProfileMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        List<MasteryStateDO> dueItems = masteryStateMapper.selectList(
                new LambdaQueryWrapper<MasteryStateDO>()
                        .eq(MasteryStateDO::getUserId, userId)
                        .le(MasteryStateDO::getNextReviewAt, LocalDateTime.now())
                        .orderByAsc(MasteryStateDO::getNextReviewAt));

        List<MasteryStateResponse> responses = new ArrayList<>();
        for (MasteryStateDO ms : dueItems) {
            LearningItemDO item = learningItemMapper.selectById(ms.getLearningItemId());
            if (item == null) continue;
            responses.add(new MasteryStateResponse(
                    ms.getLearningItemId(), item.getContent(),
                    ms.getSeenCount(), ms.getCorrectCount(), ms.getWrongCount(),
                    ms.getCorrectStreak(), ms.getRecognitionScore(), ms.getOutputScore(),
                    ms.getForgetRisk(), ms.getStatus(), ms.getRecommendedMode(),
                    ms.getLastSeenAt(), ms.getNextReviewAt()));
        }

        return new MasteryQueryResponse(responses, responses.size());
    }
}
