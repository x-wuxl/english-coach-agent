package com.wuxl.englishcoach.api.memory;

import com.wuxl.englishcoach.api.memory.dto.PriorityMemoryResponse;
import com.wuxl.englishcoach.application.memory.MemoryService;
import com.wuxl.englishcoach.common.response.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping("/priority")
    public BaseResponse<PriorityMemoryResponse> priority(@RequestParam Long userId) {
        return BaseResponse.success(memoryService.getPriorityMemory(userId));
    }
}
