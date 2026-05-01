package com.wuxl.englishcoach.api.content;

import com.wuxl.englishcoach.api.content.dto.LearningItemDetailResponse;
import com.wuxl.englishcoach.api.content.dto.LearningItemSummaryResponse;
import com.wuxl.englishcoach.application.content.ContentService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import com.wuxl.englishcoach.common.response.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/items")
    public BaseResponse<PageResponse<LearningItemSummaryResponse>> listItems(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) Integer difficultyMin,
            @RequestParam(required = false) Integer difficultyMax,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return BaseResponse.success(contentService.listItems(type, theme, difficultyMin, difficultyMax, page, size));
    }

    @GetMapping("/items/{itemId}")
    public BaseResponse<LearningItemDetailResponse> getItem(@PathVariable Long itemId) {
        return BaseResponse.success(contentService.getDetail(itemId));
    }
}
