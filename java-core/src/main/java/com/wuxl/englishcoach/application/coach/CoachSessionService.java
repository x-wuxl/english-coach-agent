package com.wuxl.englishcoach.application.coach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxl.englishcoach.api.coach.dto.CoachSessionResponse;
import com.wuxl.englishcoach.api.coach.dto.CoachTurnResponse;
import com.wuxl.englishcoach.api.coach.dto.DrillSuggestionResponse;
import com.wuxl.englishcoach.api.coach.dto.FirstCoachingSessionRequest;
import com.wuxl.englishcoach.api.coach.dto.FirstCoachingSessionResponse;
import com.wuxl.englishcoach.api.coach.dto.SavedNoteResponse;
import com.wuxl.englishcoach.api.coach.dto.SubmitCoachTurnRequest;
import com.wuxl.englishcoach.api.memory.dto.PriorityMemoryResponse;
import com.wuxl.englishcoach.application.memory.MemoryService;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.domain.memory.DrillSuggestionPolicy;
import com.wuxl.englishcoach.infrastructure.llm.PythonAgentClient;
import com.wuxl.englishcoach.infrastructure.llm.dto.CoachTurnAnalysisRequest;
import com.wuxl.englishcoach.infrastructure.llm.dto.CoachTurnAnalysisResponse;
import com.wuxl.englishcoach.infrastructure.llm.dto.ExpressionGapDto;
import com.wuxl.englishcoach.infrastructure.llm.dto.SavedNoteDto;
import com.wuxl.englishcoach.infrastructure.persistence.coach.CoachSessionDO;
import com.wuxl.englishcoach.infrastructure.persistence.coach.CoachSessionMapper;
import com.wuxl.englishcoach.infrastructure.persistence.coach.CoachTurnDO;
import com.wuxl.englishcoach.infrastructure.persistence.coach.CoachTurnMapper;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemDO;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemMapper;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanItemDO;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanItemMapper;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanSnapshotDO;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanSnapshotMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileDO;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ErrorPatternDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoachSessionService {

    private final CoachSessionMapper coachSessionMapper;
    private final CoachTurnMapper coachTurnMapper;
    private final UserProfileMapper userProfileMapper;
    private final PythonAgentClient pythonAgentClient;
    private final MemoryService memoryService;
    private final DailyPlanSnapshotMapper dailyPlanSnapshotMapper;
    private final DailyPlanItemMapper dailyPlanItemMapper;
    private final LearningItemMapper learningItemMapper;
    private final ObjectMapper objectMapper;
    private final DrillSuggestionPolicy drillSuggestionPolicy;

    public CoachSessionService(CoachSessionMapper coachSessionMapper,
                               CoachTurnMapper coachTurnMapper,
                               UserProfileMapper userProfileMapper,
                               PythonAgentClient pythonAgentClient,
                               MemoryService memoryService,
                               ObjectMapper objectMapper,
                               DailyPlanSnapshotMapper dailyPlanSnapshotMapper,
                               DailyPlanItemMapper dailyPlanItemMapper,
                               LearningItemMapper learningItemMapper) {
        this.coachSessionMapper = coachSessionMapper;
        this.coachTurnMapper = coachTurnMapper;
        this.userProfileMapper = userProfileMapper;
        this.pythonAgentClient = pythonAgentClient;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
        this.dailyPlanSnapshotMapper = dailyPlanSnapshotMapper;
        this.dailyPlanItemMapper = dailyPlanItemMapper;
        this.learningItemMapper = learningItemMapper;
        this.drillSuggestionPolicy = new DrillSuggestionPolicy();
    }

    @Transactional
    public CoachSessionResponse startSession(Long userId, String sessionType) {
        if (userProfileMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        CoachSessionDO session = new CoachSessionDO();
        session.setSessionCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        session.setUserId(userId);
        session.setSessionType(sessionType != null ? sessionType : "TODAY_COACH");
        session.setStatus("STARTED");
        session.setStartedAt(now);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        coachSessionMapper.insert(session);

        return toResponse(session);
    }

    @Transactional
    public CoachTurnResponse submitTurn(Long sessionId, SubmitCoachTurnRequest request) {
        CoachSessionDO session = coachSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCodeEnum.STUDY_SESSION_NOT_FOUND);
        }

        CoachTurnAnalysisResponse analysis = pythonAgentClient.analyzeCoachTurn(buildAnalysisRequest(session, request));
        String coachReply = analysis != null && analysis.coachReply() != null && !analysis.coachReply().isBlank()
                ? analysis.coachReply()
                : "Tell me more about that.";
        List<SavedNoteDto> savedNotes = analysis != null && analysis.savedNotes() != null
                ? analysis.savedNotes()
                : Collections.emptyList();
        List<ExpressionGapDto> expressionGaps = analysis != null && analysis.expressionGaps() != null
                ? analysis.expressionGaps()
                : Collections.emptyList();

        List<SavedNoteResponse> noteResponses = new ArrayList<>();
        DrillSuggestionResponse drillSuggestion = null;
        for (SavedNoteDto note : savedNotes) {
            noteResponses.add(new SavedNoteResponse(note.type(), note.key(), note.label(), note.userText(), note.betterText()));
            ErrorPatternDO merged = memoryService.mergeSavedErrorPattern(session.getUserId(), note);
            if (merged != null
                    && drillSuggestion == null
                    && drillSuggestionPolicy.shouldSuggest("ERROR_PATTERN", merged.getSeenCount(), merged.getStatus())) {
                drillSuggestion = new DrillSuggestionResponse("ERROR_PATTERN", merged.getId(), "Practice: " + merged.getLabel());
            }
        }
        for (ExpressionGapDto gap : expressionGaps) {
            memoryService.mergeExpressionGap(session.getUserId(), gap);
        }

        CoachTurnDO turn = new CoachTurnDO();
        turn.setCoachSessionId(sessionId);
        turn.setMode(request.mode());
        turn.setUserMessage(request.message());
        turn.setCoachMessage(coachReply);
        turn.setDetectedNotes(writeJson(savedNotes));
        turn.setCreatedAt(LocalDateTime.now());
        coachTurnMapper.insert(turn);

        PriorityMemoryResponse priorityMemory = memoryService.getPriorityMemory(session.getUserId());
        return new CoachTurnResponse(coachReply, noteResponses, priorityMemory, drillSuggestion,
                analysis != null ? analysis.fixResponse() : null);
    }


    private CoachTurnAnalysisRequest buildAnalysisRequest(CoachSessionDO session, SubmitCoachTurnRequest request) {
        return new CoachTurnAnalysisRequest(
                request.mode(),
                request.message(),
                loadRecentMessages(session.getId()),
                buildLearnerContext(session.getUserId())
        );
    }

    private List<String> loadRecentMessages(Long sessionId) {
        List<CoachTurnDO> turns = coachTurnMapper.selectList(new LambdaQueryWrapper<CoachTurnDO>()
                .eq(CoachTurnDO::getCoachSessionId, sessionId)
                .orderByDesc(CoachTurnDO::getId)
                .last("limit 6"));
        Collections.reverse(turns);
        List<String> messages = new ArrayList<>();
        for (CoachTurnDO turn : turns) {
            if (turn.getUserMessage() != null && !turn.getUserMessage().isBlank()) {
                messages.add("Learner: " + turn.getUserMessage());
            }
            if (turn.getCoachMessage() != null && !turn.getCoachMessage().isBlank()) {
                messages.add("Coach: " + turn.getCoachMessage());
            }
        }
        return messages;
    }

    private Map<String, Object> buildLearnerContext(Long userId) {
        Map<String, Object> context = new LinkedHashMap<>();
        UserProfileDO user = userProfileMapper.selectById(userId);
        if (user != null) {
            context.put("goal", user.getGoal());
            context.put("overallLevel", user.getOverallLevel());
            context.put("dailyMinutes", user.getDailyMinutes());
        }

        List<Map<String, Object>> planItems = loadTodayPlanItems(userId);
        context.put("todayPlanItems", planItems);
        context.put("currentFocus", planItems.isEmpty()
                ? (user != null ? user.getGoal() : "GENERAL")
                : planItems.get(0).get("content"));

        PriorityMemoryResponse priorityMemory = memoryService.getPriorityMemory(userId);
        context.put("priorityMemory", priorityMemory == null || priorityMemory.items() == null
                ? Collections.emptyList()
                : priorityMemory.items());
        return context;
    }

    private List<Map<String, Object>> loadTodayPlanItems(Long userId) {
        DailyPlanSnapshotDO plan = dailyPlanSnapshotMapper.selectOne(new LambdaQueryWrapper<DailyPlanSnapshotDO>()
                .eq(DailyPlanSnapshotDO::getUserId, userId)
                .eq(DailyPlanSnapshotDO::getStatus, "ACTIVE")
                .orderByDesc(DailyPlanSnapshotDO::getPlanDate)
                .orderByDesc(DailyPlanSnapshotDO::getId)
                .last("limit 1"));
        if (plan == null) {
            return Collections.emptyList();
        }

        List<DailyPlanItemDO> rows = dailyPlanItemMapper.selectList(new LambdaQueryWrapper<DailyPlanItemDO>()
                .eq(DailyPlanItemDO::getDailyPlanSnapshotId, plan.getId())
                .orderByAsc(DailyPlanItemDO::getSequenceNo)
                .last("limit 8"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (DailyPlanItemDO row : rows) {
            LearningItemDO item = learningItemMapper.selectById(row.getLearningItemId());
            if (item == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("itemId", item.getId());
            entry.put("content", item.getContent());
            entry.put("meaningZh", item.getMeaningZh());
            entry.put("theme", item.getTheme());
            entry.put("difficulty", item.getDifficulty());
            entry.put("itemRole", row.getItemRole());
            entry.put("recommendedMode", row.getRecommendedMode());
            items.add(entry.entrySet().stream()
                    .filter(e -> Objects.nonNull(e.getValue()))
                    .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll));
        }
        return items;
    }

    @Transactional
    public FirstCoachingSessionResponse completeFirstSession(FirstCoachingSessionRequest request) {
        CoachSessionResponse session = startSession(request.userId(), "FIRST_COACHING");
        String detectedLevelRange = "A2-B1";

        CoachSessionDO row = coachSessionMapper.selectById(session.id());
        row.setDetectedLevelRange(detectedLevelRange);
        row.setSummary(String.join("\n", request.samples() == null ? Collections.emptyList() : request.samples()));
        row.setUpdatedAt(LocalDateTime.now());
        coachSessionMapper.updateById(row);

        PriorityMemoryResponse initialMemory = memoryService.getPriorityMemory(request.userId());
        return new FirstCoachingSessionResponse(session.id(), session.sessionCode(), detectedLevelRange, initialMemory);
    }

    private CoachSessionResponse toResponse(CoachSessionDO session) {
        return new CoachSessionResponse(session.getId(), session.getSessionCode(), session.getUserId(),
                session.getSessionType(), session.getStatus());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
