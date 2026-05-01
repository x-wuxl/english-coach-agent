package com.wuxl.englishcoach.api.session;

import com.wuxl.englishcoach.api.session.dto.CompleteStudySessionRequest;
import com.wuxl.englishcoach.api.session.dto.StartStudySessionRequest;
import com.wuxl.englishcoach.api.session.dto.StudySessionDetailResponse;
import com.wuxl.englishcoach.api.session.dto.StudySessionStartResponse;
import com.wuxl.englishcoach.api.session.dto.SubmitAttemptsRequest;
import com.wuxl.englishcoach.application.session.StudySessionService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @PostMapping("/start")
    public BaseResponse<StudySessionStartResponse> start(@Valid @RequestBody StartStudySessionRequest request) {
        return BaseResponse.success(studySessionService.startSession(request.userId(), request.sessionType(), request.focusTheme()));
    }

    @PostMapping("/{sessionId}/attempts")
    public BaseResponse<Void> submitAttempts(@PathVariable Long sessionId,
                                              @Valid @RequestBody SubmitAttemptsRequest request) {
        studySessionService.submitAttempts(sessionId, request);
        return BaseResponse.success(null);
    }

    @PostMapping("/{sessionId}/complete")
    public BaseResponse<Void> complete(@PathVariable Long sessionId,
                                        @RequestBody(required = false) CompleteStudySessionRequest request) {
        studySessionService.completeSession(sessionId, request);
        return BaseResponse.success(null);
    }

    @GetMapping("/{sessionId}")
    public BaseResponse<StudySessionDetailResponse> getDetail(@PathVariable Long sessionId) {
        return BaseResponse.success(studySessionService.getDetail(sessionId));
    }
}
