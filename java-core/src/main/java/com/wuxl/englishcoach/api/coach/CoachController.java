package com.wuxl.englishcoach.api.coach;

import com.wuxl.englishcoach.api.coach.dto.CoachSessionResponse;
import com.wuxl.englishcoach.api.coach.dto.CoachTurnResponse;
import com.wuxl.englishcoach.api.coach.dto.FirstCoachingSessionRequest;
import com.wuxl.englishcoach.api.coach.dto.FirstCoachingSessionResponse;
import com.wuxl.englishcoach.api.coach.dto.CoachReviewResponse;
import com.wuxl.englishcoach.api.coach.dto.StartCoachSessionRequest;
import com.wuxl.englishcoach.api.coach.dto.SubmitCoachTurnRequest;
import com.wuxl.englishcoach.application.coach.CoachReviewService;
import com.wuxl.englishcoach.application.coach.CoachSessionService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final CoachSessionService coachSessionService;
    private final CoachReviewService coachReviewService;

    public CoachController(CoachSessionService coachSessionService, CoachReviewService coachReviewService) {
        this.coachSessionService = coachSessionService;
        this.coachReviewService = coachReviewService;
    }

    @PostMapping("/sessions")
    public BaseResponse<CoachSessionResponse> startSession(@Valid @RequestBody StartCoachSessionRequest request) {
        return BaseResponse.success(coachSessionService.startSession(request.userId(), request.sessionType()));
    }

    @PostMapping("/sessions/{sessionId}/turns")
    public BaseResponse<CoachTurnResponse> submitTurn(@PathVariable Long sessionId,
                                                       @Valid @RequestBody SubmitCoachTurnRequest request) {
        return BaseResponse.success(coachSessionService.submitTurn(sessionId, request));
    }

    @PostMapping("/sessions:first")
    public BaseResponse<FirstCoachingSessionResponse> firstSession(@Valid @RequestBody FirstCoachingSessionRequest request) {
        return BaseResponse.success(coachSessionService.completeFirstSession(request));
    }

    @GetMapping("/review")
    public BaseResponse<CoachReviewResponse> review(@RequestParam Long userId,
                                                     @RequestParam LocalDate startDate,
                                                     @RequestParam LocalDate endDate) {
        return BaseResponse.success(coachReviewService.getReview(userId, startDate, endDate));
    }
}
