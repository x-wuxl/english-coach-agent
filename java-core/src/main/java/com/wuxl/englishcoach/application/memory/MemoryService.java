package com.wuxl.englishcoach.application.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxl.englishcoach.api.memory.dto.PriorityMemoryItemResponse;
import com.wuxl.englishcoach.api.memory.dto.PriorityMemoryResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.domain.memory.DrillSuggestionPolicy;
import com.wuxl.englishcoach.domain.memory.MemoryPriorityPolicy;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ErrorPatternDO;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ErrorPatternMapper;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ExpressionGapDO;
import com.wuxl.englishcoach.infrastructure.persistence.memory.ExpressionGapMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import com.wuxl.englishcoach.infrastructure.llm.dto.ExpressionGapDto;
import com.wuxl.englishcoach.infrastructure.llm.dto.SavedNoteDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {

    private final ErrorPatternMapper errorPatternMapper;
    private final ExpressionGapMapper expressionGapMapper;
    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;
    private final MemoryPriorityPolicy priorityPolicy;
    private final DrillSuggestionPolicy drillSuggestionPolicy;

    public MemoryService(ErrorPatternMapper errorPatternMapper,
                         ExpressionGapMapper expressionGapMapper,
                         UserProfileMapper userProfileMapper,
                         ObjectMapper objectMapper) {
        this.errorPatternMapper = errorPatternMapper;
        this.expressionGapMapper = expressionGapMapper;
        this.userProfileMapper = userProfileMapper;
        this.objectMapper = objectMapper;
        this.priorityPolicy = new MemoryPriorityPolicy();
        this.drillSuggestionPolicy = new DrillSuggestionPolicy();
    }

    public PriorityMemoryResponse getPriorityMemory(Long userId) {
        if (userProfileMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        List<PriorityMemoryItemResponse> items = new ArrayList<>();
        items.addAll(errorPatternMapper.selectList(new LambdaQueryWrapper<ErrorPatternDO>()
                        .eq(ErrorPatternDO::getUserId, userId)
                        .eq(ErrorPatternDO::getStatus, "ACTIVE"))
                .stream()
                .map(this::toErrorPatternResponse)
                .toList());
        items.addAll(expressionGapMapper.selectList(new LambdaQueryWrapper<ExpressionGapDO>()
                        .eq(ExpressionGapDO::getUserId, userId)
                        .eq(ExpressionGapDO::getStatus, "ACTIVE"))
                .stream()
                .map(this::toExpressionGapResponse)
                .toList());

        return new PriorityMemoryResponse(items.stream()
                .sorted(Comparator.comparingDouble(PriorityMemoryItemResponse::priorityScore).reversed())
                .limit(5)
                .toList());
    }

    public ErrorPatternDO mergeSavedErrorPattern(Long userId, SavedNoteDto note) {
        ErrorPatternDO row = errorPatternMapper.selectOne(new LambdaQueryWrapper<ErrorPatternDO>()
                .eq(ErrorPatternDO::getUserId, userId)
                .eq(ErrorPatternDO::getPatternKey, note.key()));

        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            row = new ErrorPatternDO();
            row.setUserId(userId);
            row.setPatternKey(note.key());
            row.setLabel(note.label());
            row.setDescriptionZh(note.descriptionZh());
            row.setUserExamples(toJsonList(note.userText()));
            row.setBetterExamples(toJsonList(note.betterText()));
            row.setSeenCount(1);
            row.setSeverity(note.severity() != null ? note.severity() : "MEDIUM");
            row.setStatus("ACTIVE");
            row.setLastSeenAt(now);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            errorPatternMapper.insert(row);
            return row;
        }

        row.setSeenCount((row.getSeenCount() == null ? 0 : row.getSeenCount()) + 1);
        row.setLabel(note.label());
        row.setDescriptionZh(note.descriptionZh());
        row.setSeverity(note.severity() != null ? note.severity() : row.getSeverity());
        row.setUserExamples(appendJsonListValue(row.getUserExamples(), note.userText()));
        row.setBetterExamples(appendJsonListValue(row.getBetterExamples(), note.betterText()));
        row.setLastSeenAt(now);
        row.setUpdatedAt(now);
        errorPatternMapper.updateById(row);
        return row;
    }

    public ExpressionGapDO mergeExpressionGap(Long userId, ExpressionGapDto gap) {
        ExpressionGapDO row = expressionGapMapper.selectOne(new LambdaQueryWrapper<ExpressionGapDO>()
                .eq(ExpressionGapDO::getUserId, userId)
                .eq(ExpressionGapDO::getGapKey, gap.key()));

        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            row = new ExpressionGapDO();
            row.setUserId(userId);
            row.setGapKey(gap.key());
            row.setZhIntent(gap.zhIntent());
            row.setNaturalExpressions(writeJsonList(gap.naturalExpressions() == null ? List.of() : gap.naturalExpressions()));
            row.setUserAttempts(toJsonList(gap.userAttempt()));
            row.setContext(gap.context());
            row.setSeenCount(1);
            row.setStatus("ACTIVE");
            row.setLastSeenAt(now);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            expressionGapMapper.insert(row);
            return row;
        }

        row.setSeenCount((row.getSeenCount() == null ? 0 : row.getSeenCount()) + 1);
        row.setZhIntent(gap.zhIntent());
        row.setNaturalExpressions(appendJsonListValues(row.getNaturalExpressions(), gap.naturalExpressions()));
        row.setUserAttempts(appendJsonListValue(row.getUserAttempts(), gap.userAttempt()));
        row.setContext(gap.context());
        row.setLastSeenAt(now);
        row.setUpdatedAt(now);
        expressionGapMapper.updateById(row);
        return row;
    }

    private PriorityMemoryItemResponse toErrorPatternResponse(ErrorPatternDO row) {
        double score = priorityPolicy.score(new MemoryPriorityPolicy.MemorySnapshot(
                "ERROR_PATTERN", row.getId(), row.getLabel(), row.getSeenCount(), row.getSeverity(), row.getStatus(), row.getNextDrillAt()
        ));
        return new PriorityMemoryItemResponse(
                "ERROR_PATTERN",
                row.getId(),
                row.getLabel(),
                firstJsonListValue(row.getUserExamples()),
                firstJsonListValue(row.getBetterExamples()),
                row.getSeenCount(),
                row.getStatus(),
                recommendedAction("ERROR_PATTERN", row.getSeenCount(), row.getStatus()),
                row.getNextDrillAt(),
                score
        );
    }

    private PriorityMemoryItemResponse toExpressionGapResponse(ExpressionGapDO row) {
        double score = priorityPolicy.score(new MemoryPriorityPolicy.MemorySnapshot(
                "EXPRESSION_GAP", row.getId(), row.getZhIntent(), row.getSeenCount(), "MEDIUM", row.getStatus(), row.getNextDrillAt()
        ));
        return new PriorityMemoryItemResponse(
                "EXPRESSION_GAP",
                row.getId(),
                row.getZhIntent(),
                firstJsonListValue(row.getUserAttempts()),
                firstJsonListValue(row.getNaturalExpressions()),
                row.getSeenCount(),
                row.getStatus(),
                recommendedAction("EXPRESSION_GAP", row.getSeenCount(), row.getStatus()),
                row.getNextDrillAt(),
                score
        );
    }

    private String recommendedAction(String memoryType, Integer seenCount, String status) {
        return drillSuggestionPolicy.shouldSuggest(memoryType, seenCount == null ? 0 : seenCount, status)
                ? "START_DRILL"
                : "REVIEW_LATER";
    }

    private String firstJsonListValue(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<>() {});
            return values.isEmpty() ? "" : values.get(0);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    private String appendJsonListValue(String json, String value) {
        List<String> values = new ArrayList<>();
        if (json != null && !json.isBlank()) {
            try {
                values.addAll(objectMapper.readValue(json, new TypeReference<>() {}));
            } catch (JsonProcessingException ignored) {
                values.clear();
            }
        }
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
        return writeJsonList(values);
    }

    private String appendJsonListValues(String json, List<String> newValues) {
        List<String> values = new ArrayList<>();
        if (json != null && !json.isBlank()) {
            try {
                values.addAll(objectMapper.readValue(json, new TypeReference<>() {}));
            } catch (JsonProcessingException ignored) {
                values.clear();
            }
        }
        if (newValues != null) {
            for (String value : newValues) {
                if (value != null && !value.isBlank() && !values.contains(value)) {
                    values.add(value);
                }
            }
        }
        return writeJsonList(values);
    }

    private String toJsonList(String value) {
        List<String> values = new ArrayList<>();
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
        return writeJsonList(values);
    }

    private String writeJsonList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
