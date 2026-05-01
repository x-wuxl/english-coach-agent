package com.wuxl.englishcoach.api.mastery;

import com.wuxl.englishcoach.api.mastery.dto.MasteryQueryResponse;
import com.wuxl.englishcoach.application.mastery.MasteryStateService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mastery")
public class MasteryController {

    private final MasteryStateService masteryStateService;

    public MasteryController(MasteryStateService masteryStateService) {
        this.masteryStateService = masteryStateService;
    }

    @GetMapping
    public BaseResponse<MasteryQueryResponse> queryMastery(@RequestParam Long userId,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String theme) {
        return BaseResponse.success(masteryStateService.queryMastery(userId, status, theme));
    }

    @GetMapping("/due-review")
    public BaseResponse<MasteryQueryResponse> getDueReview(@RequestParam Long userId) {
        return BaseResponse.success(masteryStateService.getDueReview(userId));
    }
}
