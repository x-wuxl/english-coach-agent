package com.wuxl.englishcoach.api.session.dto;

import jakarta.validation.Valid;
import java.util.List;

public record SubmitAttemptsRequest(
        @Valid List<SubmitAttemptRequest> attempts
) {
}
