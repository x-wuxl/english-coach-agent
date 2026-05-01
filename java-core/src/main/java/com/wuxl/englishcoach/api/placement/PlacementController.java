package com.wuxl.englishcoach.api.placement;

import com.wuxl.englishcoach.api.placement.dto.PlacementAssessmentRequest;
import com.wuxl.englishcoach.api.placement.dto.PlacementAssessmentResponse;
import com.wuxl.englishcoach.application.placement.PlacementService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/placement")
public class PlacementController {

    private final PlacementService placementService;

    public PlacementController(PlacementService placementService) {
        this.placementService = placementService;
    }

    @PostMapping("/assess")
    public BaseResponse<PlacementAssessmentResponse> assess(@Valid @RequestBody PlacementAssessmentRequest request) {
        return BaseResponse.success(placementService.assess(request));
    }
}
