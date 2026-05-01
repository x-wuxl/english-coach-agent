package com.wuxl.englishcoach.api.review;

import com.wuxl.englishcoach.api.review.dto.GenerateWeeklyReviewRequest;
import com.wuxl.englishcoach.api.review.dto.WeeklyReviewResponse;
import com.wuxl.englishcoach.application.review.WeeklyReviewService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class WeeklyReviewController {

    private final WeeklyReviewService weeklyReviewService;

    public WeeklyReviewController(WeeklyReviewService weeklyReviewService) {
        this.weeklyReviewService = weeklyReviewService;
    }

    @PostMapping("/weekly:generate")
    public BaseResponse<WeeklyReviewResponse> generate(@Valid @RequestBody GenerateWeeklyReviewRequest request) {
        return BaseResponse.success(weeklyReviewService.generate(request));
    }

    @GetMapping("/weekly")
    public BaseResponse<WeeklyReviewResponse> getReview(@RequestParam Long userId,
                                                         @RequestParam String weekStartDate,
                                                         @RequestParam String weekEndDate) {
        return BaseResponse.success(weeklyReviewService.getReview(userId, weekStartDate, weekEndDate));
    }
}
