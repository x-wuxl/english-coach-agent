package com.wuxl.englishcoach.api.progress;

import com.wuxl.englishcoach.api.progress.dto.ProgressSummaryResponse;
import com.wuxl.englishcoach.application.progress.ProgressService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/summary")
    public BaseResponse<ProgressSummaryResponse> summary(@RequestParam Long userId) {
        return BaseResponse.success(progressService.summary(userId));
    }
}
