package com.wuxl.englishcoach.application.progress;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wuxl.englishcoach.api.progress.dto.ProgressSummaryResponse;
import com.wuxl.englishcoach.api.progress.dto.ProgressWeakItemResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemDO;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemMapper;
import com.wuxl.englishcoach.infrastructure.persistence.mastery.MasteryStateDO;
import com.wuxl.englishcoach.infrastructure.persistence.mastery.MasteryStateMapper;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionDO;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {

    private final UserProfileMapper userProfileMapper;
    private final MasteryStateMapper masteryStateMapper;
    private final StudySessionMapper studySessionMapper;
    private final LearningItemMapper learningItemMapper;

    public ProgressService(UserProfileMapper userProfileMapper,
                           MasteryStateMapper masteryStateMapper,
                           StudySessionMapper studySessionMapper,
                           LearningItemMapper learningItemMapper) {
        this.userProfileMapper = userProfileMapper;
        this.masteryStateMapper = masteryStateMapper;
        this.studySessionMapper = studySessionMapper;
        this.learningItemMapper = learningItemMapper;
    }

    public ProgressSummaryResponse summary(Long userId) {
        if (userProfileMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        Long totalMastery = masteryStateMapper.selectCount(new LambdaQueryWrapper<MasteryStateDO>()
                .eq(MasteryStateDO::getUserId, userId));
        Long dueReview = masteryStateMapper.selectCount(new LambdaQueryWrapper<MasteryStateDO>()
                .eq(MasteryStateDO::getUserId, userId)
                .le(MasteryStateDO::getNextReviewAt, LocalDateTime.now()));

        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        List<StudySessionDO> completedSessions = studySessionMapper.selectList(new LambdaQueryWrapper<StudySessionDO>()
                .eq(StudySessionDO::getUserId, userId)
                .eq(StudySessionDO::getStatus, "COMPLETED")
                .ge(StudySessionDO::getSessionDate, weekStart));
        BigDecimal recentAccuracy = averageAccuracy(completedSessions);

        List<MasteryStateDO> weakRows = masteryStateMapper.selectList(new LambdaQueryWrapper<MasteryStateDO>()
                .eq(MasteryStateDO::getUserId, userId)
                .gt(MasteryStateDO::getWrongCount, 0)
                .orderByDesc(MasteryStateDO::getWrongCount)
                .last("limit 5"));
        List<ProgressWeakItemResponse> weakItems = new ArrayList<>();
        for (MasteryStateDO row : weakRows) {
            LearningItemDO item = learningItemMapper.selectById(row.getLearningItemId());
            weakItems.add(new ProgressWeakItemResponse(
                    row.getLearningItemId(),
                    item != null ? item.getContent() : null,
                    item != null ? item.getMeaningZh() : null,
                    row.getWrongCount(),
                    row.getSeenCount(),
                    row.getRecommendedMode()
            ));
        }

        return new ProgressSummaryResponse(userId, totalMastery, dueReview, (long) completedSessions.size(), recentAccuracy, weakItems);
    }

    private BigDecimal averageAccuracy(List<StudySessionDO> sessions) {
        List<BigDecimal> accuracies = sessions.stream()
                .map(StudySessionDO::getAccuracy)
                .filter(value -> value != null)
                .toList();
        if (accuracies.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = accuracies.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(accuracies.size()), 2, RoundingMode.HALF_UP);
    }
}
