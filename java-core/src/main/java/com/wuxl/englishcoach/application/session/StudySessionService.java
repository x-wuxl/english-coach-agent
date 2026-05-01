package com.wuxl.englishcoach.application.session;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wuxl.englishcoach.api.session.dto.AttemptDetailResponse;
import com.wuxl.englishcoach.api.session.dto.CompleteStudySessionRequest;
import com.wuxl.englishcoach.api.session.dto.StudySessionDetailResponse;
import com.wuxl.englishcoach.api.session.dto.StudySessionStartResponse;
import com.wuxl.englishcoach.api.session.dto.SubmitAttemptsRequest;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.domain.mastery.MasteryStateMachine;
import com.wuxl.englishcoach.domain.mastery.NextReviewPolicy;
import com.wuxl.englishcoach.domain.mastery.ScoreUpdatePolicy;
import com.wuxl.englishcoach.domain.mastery.ScoreUpdatePolicy.ScoreDelta;
import com.wuxl.englishcoach.infrastructure.persistence.mastery.MasteryStateDO;
import com.wuxl.englishcoach.infrastructure.persistence.mastery.MasteryStateMapper;
import com.wuxl.englishcoach.infrastructure.persistence.session.AttemptLogDO;
import com.wuxl.englishcoach.infrastructure.persistence.session.AttemptLogMapper;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionDO;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudySessionService {

    private final UserProfileMapper userProfileMapper;
    private final StudySessionMapper studySessionMapper;
    private final AttemptLogMapper attemptLogMapper;
    private final MasteryStateMapper masteryStateMapper;
    private final ScoreUpdatePolicy scoreUpdatePolicy;
    private final MasteryStateMachine stateMachine;
    private final NextReviewPolicy nextReviewPolicy;

    public StudySessionService(UserProfileMapper userProfileMapper,
                               StudySessionMapper studySessionMapper,
                               AttemptLogMapper attemptLogMapper,
                               MasteryStateMapper masteryStateMapper,
                               ScoreUpdatePolicy scoreUpdatePolicy,
                               MasteryStateMachine stateMachine,
                               NextReviewPolicy nextReviewPolicy) {
        this.userProfileMapper = userProfileMapper;
        this.studySessionMapper = studySessionMapper;
        this.attemptLogMapper = attemptLogMapper;
        this.masteryStateMapper = masteryStateMapper;
        this.scoreUpdatePolicy = scoreUpdatePolicy;
        this.stateMachine = stateMachine;
        this.nextReviewPolicy = nextReviewPolicy;
    }

    @Transactional
    public StudySessionStartResponse startSession(Long userId, String sessionType, String focusTheme) {
        UserProfileDO user = userProfileMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        StudySessionDO session = new StudySessionDO();
        session.setSessionCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        session.setUserId(userId);
        session.setSessionDate(LocalDate.now());
        session.setSessionType(sessionType != null ? sessionType : "DAILY_LEARNING");
        session.setStatus("STARTED");
        session.setFocusTheme(focusTheme);
        session.setStartedAt(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        studySessionMapper.insert(session);

        return new StudySessionStartResponse(session.getId(), session.getSessionCode(),
                session.getSessionType(), session.getStatus());
    }

    @Transactional
    public void submitAttempts(Long sessionId, SubmitAttemptsRequest request) {
        StudySessionDO session = studySessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCodeEnum.STUDY_SESSION_NOT_FOUND);
        }
        if ("COMPLETED".equals(session.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.SESSION_ALREADY_COMPLETED);
        }

        for (var attemptReq : request.attempts()) {
            // Persist attempt
            AttemptLogDO attempt = new AttemptLogDO();
            attempt.setAttemptCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            attempt.setUserId(session.getUserId());
            attempt.setLearningItemId(attemptReq.learningItemId());
            attempt.setStudySessionId(sessionId);
            attempt.setMode(attemptReq.mode());
            attempt.setResult(attemptReq.result());
            attempt.setResponseText(attemptReq.responseText());
            attempt.setResponseTimeMs(attemptReq.responseTimeMs());
            attempt.setHintUsed(Boolean.TRUE.equals(attemptReq.hintUsed()));
            attempt.setErrorType(attemptReq.errorType());
            attempt.setOccurredAt(LocalDateTime.now());
            attempt.setCreatedAt(LocalDateTime.now());
            attemptLogMapper.insert(attempt);

            // Update mastery state
            updateMasteryState(session.getUserId(), attemptReq.learningItemId(),
                    "CORRECT".equals(attemptReq.result()),
                    Boolean.TRUE.equals(attemptReq.hintUsed()),
                    attemptReq.mode());
        }
    }

    @Transactional
    public void completeSession(Long sessionId, CompleteStudySessionRequest request) {
        StudySessionDO session = studySessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCodeEnum.STUDY_SESSION_NOT_FOUND);
        }
        if ("COMPLETED".equals(session.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.SESSION_ALREADY_COMPLETED);
        }

        // Calculate stats from attempts
        List<AttemptLogDO> attempts = attemptLogMapper.selectList(
                new LambdaQueryWrapper<AttemptLogDO>().eq(AttemptLogDO::getStudySessionId, sessionId));

        long correct = attempts.stream().filter(a -> "CORRECT".equals(a.getResult())).count();
        BigDecimal accuracy = attempts.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf((double) correct / attempts.size()).setScale(2, RoundingMode.HALF_UP);

        session.setStatus("COMPLETED");
        session.setAccuracy(accuracy);
        session.setCompletionRate(BigDecimal.ONE); // simplified: all submitted = completed
        session.setFatigueFeedback(request != null ? request.fatigueFeedback() : null);
        session.setMoodFeedback(request != null ? request.moodFeedback() : null);
        session.setCompletedAt(LocalDateTime.now());

        if (session.getStartedAt() != null) {
            session.setDurationMin((int) ChronoUnit.MINUTES.between(session.getStartedAt(), LocalDateTime.now()));
        }
        session.setUpdatedAt(LocalDateTime.now());
        studySessionMapper.updateById(session);
    }

    public StudySessionDetailResponse getDetail(Long sessionId) {
        StudySessionDO session = studySessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCodeEnum.STUDY_SESSION_NOT_FOUND);
        }

        List<AttemptLogDO> attempts = attemptLogMapper.selectList(
                new LambdaQueryWrapper<AttemptLogDO>().eq(AttemptLogDO::getStudySessionId, sessionId));

        List<AttemptDetailResponse> attemptResponses = attempts.stream()
                .map(a -> new AttemptDetailResponse(a.getId(), a.getAttemptCode(), a.getLearningItemId(),
                        a.getMode(), a.getResult(), a.getResponseText(), a.getResponseTimeMs(),
                        a.getHintUsed(), a.getErrorType()))
                .toList();

        return new StudySessionDetailResponse(
                session.getId(), session.getSessionCode(), session.getUserId(),
                session.getSessionType(), session.getStatus(), session.getDurationMin(),
                session.getNewItemsCount(), session.getReviewItemsCount(),
                session.getAccuracy(), session.getCompletionRate(), attemptResponses);
    }

    private void updateMasteryState(Long userId, Long learningItemId, boolean correct,
                                     boolean hintUsed, String mode) {
        MasteryStateDO mastery = masteryStateMapper.selectOne(
                new LambdaQueryWrapper<MasteryStateDO>()
                        .eq(MasteryStateDO::getUserId, userId)
                        .eq(MasteryStateDO::getLearningItemId, learningItemId));

        if (mastery == null) {
            mastery = new MasteryStateDO();
            mastery.setUserId(userId);
            mastery.setLearningItemId(learningItemId);
            mastery.setSeenCount(0);
            mastery.setCorrectCount(0);
            mastery.setWrongCount(0);
            mastery.setCorrectStreak(0);
            mastery.setRecognitionScore(BigDecimal.ZERO);
            mastery.setRecallScore(BigDecimal.ZERO);
            mastery.setOutputScore(BigDecimal.ZERO);
            mastery.setMemoryStrength(BigDecimal.ZERO);
            mastery.setForgetRisk(BigDecimal.valueOf(0.5));
            mastery.setStatus("NEW");
            mastery.setCreatedAt(LocalDateTime.now());
        }

        // Update counts
        mastery.setSeenCount(mastery.getSeenCount() + 1);
        if (correct) {
            mastery.setCorrectCount(mastery.getCorrectCount() + 1);
            mastery.setCorrectStreak(mastery.getCorrectStreak() + 1);
        } else {
            mastery.setWrongCount(mastery.getWrongCount() + 1);
            mastery.setCorrectStreak(0);
        }

        // Apply score deltas
        ScoreDelta delta = scoreUpdatePolicy.calculate(correct, hintUsed, mode);
        BigDecimal newRecognition = clamp(mastery.getRecognitionScore().add(delta.recognitionDelta()));
        BigDecimal newOutput = clamp(mastery.getOutputScore().add(delta.outputDelta()));
        BigDecimal newForgetRisk = clamp(mastery.getForgetRisk().add(delta.forgetRiskDelta()));

        mastery.setRecognitionScore(newRecognition);
        mastery.setOutputScore(newOutput);
        mastery.setForgetRisk(newForgetRisk);

        // State transition
        String newStatus = stateMachine.transition(mastery.getStatus(), newRecognition, newOutput,
                mastery.getCorrectStreak(), correct ? 0 : mastery.getWrongCount());
        mastery.setStatus(newStatus);

        // Next review time
        mastery.setNextReviewAt(nextReviewPolicy.calculateNextReview(newStatus, mastery.getCorrectStreak(), correct));
        mastery.setLastSeenAt(LocalDateTime.now());
        mastery.setLastMode(mode);
        mastery.setUpdatedAt(LocalDateTime.now());

        if (mastery.getId() == null) {
            masteryStateMapper.insert(mastery);
        } else {
            masteryStateMapper.updateById(mastery);
        }
    }

    private BigDecimal clamp(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}
