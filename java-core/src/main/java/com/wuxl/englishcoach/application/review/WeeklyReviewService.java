package com.wuxl.englishcoach.application.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxl.englishcoach.api.review.dto.GenerateWeeklyReviewRequest;
import com.wuxl.englishcoach.api.review.dto.WeeklyReviewResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.domain.review.WeeklyReviewAggregator;
import com.wuxl.englishcoach.domain.review.WeeklyReviewAggregator.AttemptData;
import com.wuxl.englishcoach.domain.review.WeeklyReviewAggregator.ReviewResult;
import com.wuxl.englishcoach.domain.review.WeeklyReviewAggregator.SessionData;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanSnapshotDO;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanSnapshotMapper;
import com.wuxl.englishcoach.infrastructure.persistence.review.WeeklyReviewSnapshotDO;
import com.wuxl.englishcoach.infrastructure.persistence.review.WeeklyReviewSnapshotMapper;
import com.wuxl.englishcoach.infrastructure.persistence.session.AttemptLogDO;
import com.wuxl.englishcoach.infrastructure.persistence.session.AttemptLogMapper;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionDO;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyReviewService {

    private final UserProfileMapper userProfileMapper;
    private final StudySessionMapper studySessionMapper;
    private final AttemptLogMapper attemptLogMapper;
    private final DailyPlanSnapshotMapper planSnapshotMapper;
    private final WeeklyReviewSnapshotMapper reviewSnapshotMapper;
    private final WeeklyReviewAggregator aggregator;
    private final ObjectMapper objectMapper;

    public WeeklyReviewService(UserProfileMapper userProfileMapper,
                               StudySessionMapper studySessionMapper,
                               AttemptLogMapper attemptLogMapper,
                               DailyPlanSnapshotMapper planSnapshotMapper,
                               WeeklyReviewSnapshotMapper reviewSnapshotMapper,
                               WeeklyReviewAggregator aggregator,
                               ObjectMapper objectMapper) {
        this.userProfileMapper = userProfileMapper;
        this.studySessionMapper = studySessionMapper;
        this.attemptLogMapper = attemptLogMapper;
        this.planSnapshotMapper = planSnapshotMapper;
        this.reviewSnapshotMapper = reviewSnapshotMapper;
        this.aggregator = aggregator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WeeklyReviewResponse generate(GenerateWeeklyReviewRequest request) {
        UserProfileDO user = userProfileMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        LocalDate weekStart = LocalDate.parse(request.weekStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate weekEnd = LocalDate.parse(request.weekEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        // Check duplicate
        LambdaQueryWrapper<WeeklyReviewSnapshotDO> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(WeeklyReviewSnapshotDO::getUserId, request.userId())
                .eq(WeeklyReviewSnapshotDO::getWeekStartDate, weekStart)
                .eq(WeeklyReviewSnapshotDO::getWeekEndDate, weekEnd);
        if (reviewSnapshotMapper.selectCount(dupCheck) > 0) {
            throw new BusinessException(ErrorCodeEnum.DUPLICATE_DAILY_PLAN); // reuse error code
        }

        // Gather sessions
        List<StudySessionDO> sessions = studySessionMapper.selectList(
                new LambdaQueryWrapper<StudySessionDO>()
                        .eq(StudySessionDO::getUserId, request.userId())
                        .ge(StudySessionDO::getSessionDate, weekStart)
                        .le(StudySessionDO::getSessionDate, weekEnd));

        List<SessionData> sessionData = sessions.stream()
                .map(s -> new SessionData(
                        s.getDurationMin() != null ? s.getDurationMin() : 0,
                        s.getAccuracy() != null ? s.getAccuracy() : java.math.BigDecimal.ZERO,
                        s.getStatus(), s.getSessionType()))
                .toList();

        // Gather attempts
        List<Long> sessionIds = sessions.stream().map(StudySessionDO::getId).toList();
        List<AttemptLogDO> attempts = sessionIds.isEmpty() ? Collections.emptyList() :
                attemptLogMapper.selectList(
                        new LambdaQueryWrapper<AttemptLogDO>()
                                .in(AttemptLogDO::getStudySessionId, sessionIds));

        List<AttemptData> attemptData = attempts.stream()
                .map(a -> new AttemptData(a.getResult(), a.getErrorType(), null))
                .toList();

        // Count plans
        Long generatedPlanCountLong = planSnapshotMapper.selectCount(
                new LambdaQueryWrapper<DailyPlanSnapshotDO>()
                        .eq(DailyPlanSnapshotDO::getUserId, request.userId())
                        .ge(DailyPlanSnapshotDO::getPlanDate, weekStart)
                        .le(DailyPlanSnapshotDO::getPlanDate, weekEnd));
        int generatedPlanCount = generatedPlanCountLong != null ? generatedPlanCountLong.intValue() : 0;

        // Aggregate
        ReviewResult result = aggregator.aggregate(sessionData, attemptData, generatedPlanCount, generatedPlanCount);

        // Persist
        WeeklyReviewSnapshotDO snapshot = new WeeklyReviewSnapshotDO();
        snapshot.setUserId(request.userId());
        snapshot.setWeekStartDate(weekStart);
        snapshot.setWeekEndDate(weekEnd);
        snapshot.setCompletionRate(result.completionRate());
        snapshot.setStudyMinutes(result.studyMinutes());
        snapshot.setNewItemsCount(result.newItemsCount());
        snapshot.setReviewItemsCount(result.reviewItemsCount());
        snapshot.setHighFrequencyErrorTypes(toJson(result.highFrequencyErrorTypes()));
        snapshot.setStrongestThemes(toJson(result.strongestThemes()));
        snapshot.setWeakestThemes(toJson(result.weakestThemes()));
        snapshot.setNextWeekSuggestion(result.nextWeekSuggestion());
        snapshot.setCreatedAt(LocalDateTime.now());
        reviewSnapshotMapper.insert(snapshot);

        return toResponse(snapshot);
    }

    public WeeklyReviewResponse getReview(Long userId, String weekStartDate, String weekEndDate) {
        LocalDate weekStart = LocalDate.parse(weekStartDate, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate weekEnd = LocalDate.parse(weekEndDate, DateTimeFormatter.ISO_LOCAL_DATE);

        LambdaQueryWrapper<WeeklyReviewSnapshotDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeeklyReviewSnapshotDO::getUserId, userId)
                .eq(WeeklyReviewSnapshotDO::getWeekStartDate, weekStart)
                .eq(WeeklyReviewSnapshotDO::getWeekEndDate, weekEnd);
        WeeklyReviewSnapshotDO snapshot = reviewSnapshotMapper.selectOne(wrapper);
        if (snapshot == null) {
            throw new BusinessException(ErrorCodeEnum.WEEKLY_REVIEW_NOT_FOUND);
        }
        return toResponse(snapshot);
    }

    private WeeklyReviewResponse toResponse(WeeklyReviewSnapshotDO snapshot) {
        return new WeeklyReviewResponse(
                snapshot.getUserId(),
                snapshot.getWeekStartDate().toString(),
                snapshot.getWeekEndDate().toString(),
                snapshot.getCompletionRate(),
                snapshot.getStudyMinutes(),
                snapshot.getNewItemsCount(),
                snapshot.getReviewItemsCount(),
                fromJsonList(snapshot.getHighFrequencyErrorTypes()),
                fromJsonList(snapshot.getStrongestThemes()),
                fromJsonList(snapshot.getWeakestThemes()),
                snapshot.getNextWeekSuggestion());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
