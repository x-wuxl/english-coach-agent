package com.wuxl.englishcoach.application.coach;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wuxl.englishcoach.api.coach.dto.CoachReviewResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.infrastructure.persistence.coach.CoachSessionDO;
import com.wuxl.englishcoach.infrastructure.persistence.coach.CoachSessionMapper;
import com.wuxl.englishcoach.infrastructure.persistence.coach.CoachTurnDO;
import com.wuxl.englishcoach.infrastructure.persistence.coach.CoachTurnMapper;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ErrorPatternDO;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ErrorPatternMapper;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ExpressionGapDO;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ExpressionGapMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CoachReviewService {

    private final UserProfileMapper userProfileMapper;
    private final CoachSessionMapper coachSessionMapper;
    private final CoachTurnMapper coachTurnMapper;
    private final ErrorPatternMapper errorPatternMapper;
    private final ExpressionGapMapper expressionGapMapper;

    public CoachReviewService(UserProfileMapper userProfileMapper,
                              CoachSessionMapper coachSessionMapper,
                              CoachTurnMapper coachTurnMapper,
                              ErrorPatternMapper errorPatternMapper,
                              ExpressionGapMapper expressionGapMapper) {
        this.userProfileMapper = userProfileMapper;
        this.coachSessionMapper = coachSessionMapper;
        this.coachTurnMapper = coachTurnMapper;
        this.errorPatternMapper = errorPatternMapper;
        this.expressionGapMapper = expressionGapMapper;
    }

    public CoachReviewResponse getReview(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userProfileMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Long> sessionIds = coachSessionMapper.selectList(new LambdaQueryWrapper<CoachSessionDO>()
                        .eq(CoachSessionDO::getUserId, userId)
                        .ge(CoachSessionDO::getStartedAt, start)
                        .lt(CoachSessionDO::getStartedAt, end))
                .stream()
                .map(CoachSessionDO::getId)
                .toList();
        int turns = sessionIds.isEmpty() ? 0 : Math.toIntExact(coachTurnMapper.selectCount(new LambdaQueryWrapper<CoachTurnDO>()
                .in(CoachTurnDO::getCoachSessionId, sessionIds)));

        List<ErrorPatternDO> errorPatterns = errorPatternMapper.selectList(new LambdaQueryWrapper<ErrorPatternDO>()
                .eq(ErrorPatternDO::getUserId, userId)
                .orderByDesc(ErrorPatternDO::getSeenCount)
                .last("limit 5"));
        List<ExpressionGapDO> gaps = expressionGapMapper.selectList(new LambdaQueryWrapper<ExpressionGapDO>()
                .eq(ExpressionGapDO::getUserId, userId)
                .orderByDesc(ExpressionGapDO::getSeenCount)
                .last("limit 5"));

        List<String> topProblems = errorPatterns.stream().map(ErrorPatternDO::getLabel).toList();
        List<String> improvedExpressions = gaps.stream().map(ExpressionGapDO::getZhIntent).toList();
        int newMemoryCount = errorPatterns.size() + gaps.size();
        String nextWeekPlan = turns == 0
                ? "Start with three short coach conversations and save repeated patterns."
                : "Review the top repeated patterns, then do short chat practice every day.";
        return new CoachReviewResponse(turns, newMemoryCount, topProblems, improvedExpressions, nextWeekPlan);
    }
}
