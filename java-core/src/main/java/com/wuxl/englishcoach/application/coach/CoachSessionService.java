package com.wuxl.englishcoach.application.coach;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.wuxl.englishcoach.infrastructure.persistence.memory.ErrorPatternDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final ObjectMapper objectMapper;
    private final DrillSuggestionPolicy drillSuggestionPolicy;

    public CoachSessionService(CoachSessionMapper coachSessionMapper,
                               CoachTurnMapper coachTurnMapper,
                               UserProfileMapper userProfileMapper,
                               PythonAgentClient pythonAgentClient,
                               MemoryService memoryService,
                               ObjectMapper objectMapper) {
        this.coachSessionMapper = coachSessionMapper;
        this.coachTurnMapper = coachTurnMapper;
        this.userProfileMapper = userProfileMapper;
        this.pythonAgentClient = pythonAgentClient;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
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

        CoachTurnAnalysisResponse analysis = pythonAgentClient.analyzeCoachTurn(new CoachTurnAnalysisRequest(
                request.mode(),
                request.message(),
                Collections.emptyList()
        ));
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
