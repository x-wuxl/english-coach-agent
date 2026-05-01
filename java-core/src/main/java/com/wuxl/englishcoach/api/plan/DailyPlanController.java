package com.wuxl.englishcoach.api.plan;

import com.wuxl.englishcoach.api.plan.dto.DailyPlanResponse;
import com.wuxl.englishcoach.api.plan.dto.GenerateDailyPlanRequest;
import com.wuxl.englishcoach.application.plan.DailyPlanService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class DailyPlanController {

    private final DailyPlanService dailyPlanService;

    public DailyPlanController(DailyPlanService dailyPlanService) {
        this.dailyPlanService = dailyPlanService;
    }

    @PostMapping("/daily:generate")
    public BaseResponse<DailyPlanResponse> generate(@Valid @RequestBody GenerateDailyPlanRequest request) {
        return BaseResponse.success(dailyPlanService.generate(request));
    }

    @GetMapping("/daily")
    public BaseResponse<DailyPlanResponse> getPlan(@RequestParam Long userId,
                                                    @RequestParam String planDate,
                                                    @RequestParam(required = false) String planType) {
        return BaseResponse.success(dailyPlanService.getPlan(userId, planDate, planType));
    }
}
