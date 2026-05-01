package com.wuxl.englishcoach.application.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxl.englishcoach.api.content.dto.LearningItemDetailResponse;
import com.wuxl.englishcoach.api.content.dto.LearningItemSummaryResponse;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.common.response.PageResponse;
import com.wuxl.englishcoach.domain.content.LearningItem;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemDO;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemMapper;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ContentService {

    private final LearningItemMapper learningItemMapper;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ContentService(LearningItemMapper learningItemMapper, ObjectMapper objectMapper) {
        this.learningItemMapper = learningItemMapper;
        this.objectMapper = objectMapper;
    }

    public PageResponse<LearningItemSummaryResponse> listItems(String type, String theme,
                                                                Integer difficultyMin, Integer difficultyMax,
                                                                int page, int size) {
        LambdaQueryWrapper<LearningItemDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningItemDO::getStatus, "ACTIVE");
        if (type != null) {
            wrapper.eq(LearningItemDO::getType, type);
        }
        if (theme != null) {
            wrapper.eq(LearningItemDO::getTheme, theme);
        }
        if (difficultyMin != null) {
            wrapper.ge(LearningItemDO::getDifficulty, difficultyMin);
        }
        if (difficultyMax != null) {
            wrapper.le(LearningItemDO::getDifficulty, difficultyMax);
        }
        wrapper.orderByAsc(LearningItemDO::getDifficulty, LearningItemDO::getId);

        Page<LearningItemDO> pageResult = learningItemMapper.selectPage(new Page<>(page, size), wrapper);

        List<LearningItemSummaryResponse> items = pageResult.getRecords().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new PageResponse<>(items, (int) pageResult.getCurrent(), (int) pageResult.getSize(),
                pageResult.getTotal(), (int) pageResult.getPages());
    }

    public LearningItemDetailResponse getDetail(Long itemId) {
        LearningItemDO itemDO = learningItemMapper.selectById(itemId);
        if (itemDO == null || !"ACTIVE".equals(itemDO.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.LEARNING_ITEM_NOT_FOUND);
        }
        return toDetailResponse(toDomain(itemDO));
    }

    private LearningItemSummaryResponse toSummaryResponse(LearningItemDO itemDO) {
        return new LearningItemSummaryResponse(
                itemDO.getId(), itemDO.getItemCode(), itemDO.getType(),
                itemDO.getContent(), itemDO.getMeaningZh(),
                itemDO.getDifficulty(), itemDO.getTheme(), itemDO.getStatus()
        );
    }

    private LearningItemDetailResponse toDetailResponse(LearningItem item) {
        return new LearningItemDetailResponse(
                item.id(), item.itemCode(), item.type(), item.content(), item.meaningZh(),
                item.difficulty(), item.theme(), item.tags(), item.examples(),
                item.relatedItemCodes(), item.status(), null, null
        );
    }

    private LearningItem toDomain(LearningItemDO itemDO) {
        return new LearningItem(
                itemDO.getId(), itemDO.getItemCode(), itemDO.getType(),
                itemDO.getContent(), itemDO.getMeaningZh(),
                itemDO.getDifficulty(), itemDO.getTheme(),
                fromJsonList(itemDO.getTags()),
                fromJsonMapList(itemDO.getExamples()),
                fromJsonList(itemDO.getRelatedItemCodes()),
                itemDO.getStatus()
        );
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> fromJsonMapList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
