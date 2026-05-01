package com.wuxl.englishcoach.api.mastery.dto;

import java.util.List;

public record MasteryQueryResponse(
        List<MasteryStateResponse> items,
        int total
) {
}
