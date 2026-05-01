package com.wuxl.englishcoach.application.placement;

import com.wuxl.englishcoach.api.placement.dto.PlacementAssessmentRequest;
import com.wuxl.englishcoach.api.placement.dto.PlacementAssessmentResponse;
import com.wuxl.englishcoach.api.placement.dto.PlacementAnswerRequest;
import com.wuxl.englishcoach.api.placement.dto.SuggestedDailyRhythmResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.domain.placement.PlacementScorer;
import com.wuxl.englishcoach.domain.placement.PlacementScorer.AnswerInput;
import com.wuxl.englishcoach.domain.placement.PlacementScorer.AssessmentResult;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionDO;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlacementService {

    private final UserProfileMapper userProfileMapper;
    private final StudySessionMapper studySessionMapper;
    private final PlacementScorer scorer;

    public PlacementService(UserProfileMapper userProfileMapper,
                            StudySessionMapper studySessionMapper,
                            PlacementScorer scorer) {
        this.userProfileMapper = userProfileMapper;
        this.studySessionMapper = studySessionMapper;
        this.scorer = scorer;
    }

    @Transactional
    public PlacementAssessmentResponse assess(PlacementAssessmentRequest request) {
        if (request.answers() == null || request.answers().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.EMPTY_PLACEMENT_SUBMISSION);
        }

        UserProfileDO user = userProfileMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        // Score
        var answerInputs = request.answers().stream()
                .map(a -> new AnswerInput(a.section(), a.result(),
                        Boolean.TRUE.equals(a.hintUsed()),
                        a.responseTimeMs() != null ? a.responseTimeMs() : 0))
                .toList();

        AssessmentResult result = scorer.assess(answerInputs);

        // Update user profile levels
        user.setOverallLevel(result.overallLevel());
        user.setVocabLevel(result.vocabLevel());
        user.setGrammarLevel(result.grammarLevel());
        user.setReadingLevel(result.readingLevel());
        user.setOutputLevel(result.outputLevel());
        userProfileMapper.updateById(user);

        // Create placement session
        StudySessionDO session = new StudySessionDO();
        session.setSessionCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        session.setUserId(request.userId());
        session.setSessionDate(LocalDate.now());
        session.setSessionType("PLACEMENT");
        session.setStatus("COMPLETED");
        session.setNewItemsCount(request.answers().size());
        session.setStartedAt(java.time.LocalDateTime.now());
        session.setCompletedAt(java.time.LocalDateTime.now());
        studySessionMapper.insert(session);

        return new PlacementAssessmentResponse(
                result.overallLevel(), result.vocabLevel(), result.grammarLevel(),
                result.readingLevel(), result.outputLevel(), result.weaknesses(),
                new SuggestedDailyRhythmResponse(result.suggestedNewItems(),
                        result.suggestedReviewItems(), result.suggestedOutputTasks())
        );
    }
}
