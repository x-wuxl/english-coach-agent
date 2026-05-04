package com.wuxl.englishcoach.application.plan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxl.englishcoach.api.plan.dto.DailyPlanItemResponse;
import com.wuxl.englishcoach.api.plan.dto.DailyPlanRationaleResponse;
import com.wuxl.englishcoach.api.plan.dto.DailyPlanResponse;
import com.wuxl.englishcoach.api.plan.dto.GenerateDailyPlanRequest;
import com.wuxl.englishcoach.common.enums.ErrorCodeEnum;
import com.wuxl.englishcoach.common.enums.PlanType;
import com.wuxl.englishcoach.common.exception.BusinessException;
import com.wuxl.englishcoach.domain.plan.DailyLoadPolicy;
import com.wuxl.englishcoach.domain.plan.DailyLoadPolicy.LoadResult;
import com.wuxl.englishcoach.domain.plan.NewItemSelectionPolicy;
import com.wuxl.englishcoach.domain.plan.NewItemSelectionPolicy.ItemCandidate;
import com.wuxl.englishcoach.domain.plan.ReviewPriorityCalculator;
import com.wuxl.englishcoach.domain.plan.ReviewPriorityCalculator.MasterySnapshot;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemDO;
import com.wuxl.englishcoach.infrastructure.persistence.content.LearningItemMapper;
import com.wuxl.englishcoach.infrastructure.persistence.mastery.MasteryStateDO;
import com.wuxl.englishcoach.infrastructure.persistence.mastery.MasteryStateMapper;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanItemDO;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanItemMapper;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanSnapshotDO;
import com.wuxl.englishcoach.infrastructure.persistence.plan.DailyPlanSnapshotMapper;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionDO;
import com.wuxl.englishcoach.infrastructure.persistence.session.StudySessionMapper;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileDO;
import com.wuxl.englishcoach.infrastructure.persistence.user.UserProfileMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyPlanService {

    private final UserProfileMapper userProfileMapper;
    private final LearningItemMapper learningItemMapper;
    private final MasteryStateMapper masteryStateMapper;
    private final DailyPlanSnapshotMapper planSnapshotMapper;
    private final DailyPlanItemMapper planItemMapper;
    private final StudySessionMapper studySessionMapper;
    private final DailyLoadPolicy loadPolicy;
    private final ReviewPriorityCalculator priorityCalculator;
    private final NewItemSelectionPolicy selectionPolicy;
    private final ObjectMapper objectMapper;

    public DailyPlanService(UserProfileMapper userProfileMapper,
                            LearningItemMapper learningItemMapper,
                            MasteryStateMapper masteryStateMapper,
                            DailyPlanSnapshotMapper planSnapshotMapper,
                            DailyPlanItemMapper planItemMapper,
                            StudySessionMapper studySessionMapper,
                            DailyLoadPolicy loadPolicy,
                            ReviewPriorityCalculator priorityCalculator,
                            NewItemSelectionPolicy selectionPolicy,
                            ObjectMapper objectMapper) {
        this.userProfileMapper = userProfileMapper;
        this.learningItemMapper = learningItemMapper;
        this.masteryStateMapper = masteryStateMapper;
        this.planSnapshotMapper = planSnapshotMapper;
        this.planItemMapper = planItemMapper;
        this.studySessionMapper = studySessionMapper;
        this.loadPolicy = loadPolicy;
        this.priorityCalculator = priorityCalculator;
        this.selectionPolicy = selectionPolicy;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DailyPlanResponse ensure(GenerateDailyPlanRequest request) {
        UserProfileDO user = userProfileMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        LocalDate planDate = LocalDate.parse(request.planDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        String planType = request.planType() != null ? request.planType() : PlanType.NORMAL.name();

        DailyPlanSnapshotDO existing = findPlanSnapshot(request.userId(), planDate, planType);
        if (existing != null) {
            DailyPlanResponse response = toResponse(existing, request.planDate());
            if (hasVisiblePlanItems(response)) {
                return response;
            }
            deletePlanSnapshot(existing.getId());
        }

        return createPlan(request, user, planDate, planType);
    }

    @Transactional
    public DailyPlanResponse generate(GenerateDailyPlanRequest request) {
        // Validate user
        UserProfileDO user = userProfileMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        LocalDate planDate = LocalDate.parse(request.planDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        String planType = request.planType() != null ? request.planType() : PlanType.NORMAL.name();

        // Check duplicate
        LambdaQueryWrapper<DailyPlanSnapshotDO> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(DailyPlanSnapshotDO::getUserId, request.userId())
                .eq(DailyPlanSnapshotDO::getPlanDate, planDate)
                .eq(DailyPlanSnapshotDO::getPlanType, planType);
        if (planSnapshotMapper.selectCount(dupCheck) > 0) {
            throw new BusinessException(ErrorCodeEnum.DUPLICATE_DAILY_PLAN);
        }

        return createPlan(request, user, planDate, planType);
    }

    private DailyPlanResponse createPlan(GenerateDailyPlanRequest request, UserProfileDO user,
                                         LocalDate planDate, String planType) {
        // Calculate load
        double completionRate = calcRecentCompletionRate(request.userId());
        double accuracy = calcRecentAccuracy(request.userId());
        double fatigueRatio = 0.0; // placeholder — no fatigue signal in v1
        long overdueCount = countOverdueItems(request.userId());

        LoadResult load = loadPolicy.calculate(
                user.getDailyMinutes() != null ? user.getDailyMinutes() : 15,
                completionRate, accuracy, fatigueRatio, overdueCount);

        // Get mastery states for this user
        List<MasteryStateDO> masteryStates = masteryStateMapper.selectList(
                new LambdaQueryWrapper<MasteryStateDO>().eq(MasteryStateDO::getUserId, request.userId()));

        Set<Long> masteredItemIds = masteryStates.stream()
                .map(MasteryStateDO::getLearningItemId)
                .collect(Collectors.toSet());

        // Select review items (due items sorted by priority)
        List<MasterySnapshot> snapshots = masteryStates.stream()
                .map(this::toMasterySnapshot)
                .sorted(Comparator.comparingDouble(priorityCalculator::calculatePriority).reversed())
                .toList();

        List<DailyPlanItemResponse> reviewItems = new ArrayList<>();
        List<String> whyReviewThese = new ArrayList<>();
        for (MasterySnapshot s : snapshots) {
            if (reviewItems.size() >= load.reviewCount()) break;
            double priority = priorityCalculator.calculatePriority(s);
            LearningItemDO item = learningItemMapper.selectById(s.learningItemId());
            if (item == null) continue;
            reviewItems.add(toItemResponse(item, "REVIEW", null,
                    BigDecimal.valueOf(priority), "priority=" + String.format("%.2f", priority)));
            whyReviewThese.add(item.getContent() + " priority=" + String.format("%.2f", priority));
        }

        // Select new items
        List<String> preferredThemes = fromJsonList(user.getSubGoals());
        List<LearningItemDO> candidateItems = selectBoundedActiveCandidates(preferredThemes);

        List<ItemCandidate> candidates = candidateItems.stream()
                .map(li -> new ItemCandidate(li.getId(), li.getType(), li.getTheme(), li.getDifficulty()))
                .toList();

        List<ItemCandidate> selected = selectionPolicy.select(
                candidates, masteredItemIds, preferredThemes, load.newCount());

        List<DailyPlanItemResponse> newItems = new ArrayList<>();
        for (ItemCandidate c : selected) {
            LearningItemDO item = learningItemMapper.selectById(c.id());
            if (item == null) continue;
            newItems.add(toItemResponse(item, "NEW", null, null, "new selection"));
        }

        // Persist plan
        DailyPlanSnapshotDO snapshot = new DailyPlanSnapshotDO();
        snapshot.setPlanCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        snapshot.setUserId(request.userId());
        snapshot.setPlanDate(planDate);
        snapshot.setPlanType(planType);
        snapshot.setStatus("ACTIVE");
        snapshot.setTotalNewCount(newItems.size());
        snapshot.setTotalReviewCount(reviewItems.size());
        snapshot.setTotalOutputCount(load.outputCount());
        snapshot.setLoadReason(load.loadReason());
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setUpdatedAt(LocalDateTime.now());
        planSnapshotMapper.insert(snapshot);

        // Persist plan items
        int seq = 1;
        for (DailyPlanItemResponse item : newItems) {
            DailyPlanItemDO itemDO = new DailyPlanItemDO();
            itemDO.setDailyPlanSnapshotId(snapshot.getId());
            itemDO.setLearningItemId(item.itemId());
            itemDO.setItemRole("NEW");
            itemDO.setSequenceNo(seq++);
            itemDO.setSelectionReason(item.selectionReason());
            itemDO.setCreatedAt(LocalDateTime.now());
            planItemMapper.insert(itemDO);
        }
        for (DailyPlanItemResponse item : reviewItems) {
            DailyPlanItemDO itemDO = new DailyPlanItemDO();
            itemDO.setDailyPlanSnapshotId(snapshot.getId());
            itemDO.setLearningItemId(item.itemId());
            itemDO.setItemRole("REVIEW");
            itemDO.setSequenceNo(seq++);
            itemDO.setSelectionReason(item.selectionReason());
            itemDO.setPriorityScore(item.priorityScore());
            itemDO.setCreatedAt(LocalDateTime.now());
            planItemMapper.insert(itemDO);
        }

        DailyPlanRationaleResponse rationale = new DailyPlanRationaleResponse(
                load.loadDecision(), whyReviewThese,
                "base tier " + (user.getDailyMinutes() != null ? user.getDailyMinutes() : 15) + "min, adjusted to " + load.newCount());

        return new DailyPlanResponse(
                snapshot.getPlanCode(), request.planDate(), planType, "ACTIVE",
                newItems, reviewItems, rationale);
    }

    public DailyPlanResponse getPlan(Long userId, String planDate, String planType) {
        LocalDate date = LocalDate.parse(planDate, DateTimeFormatter.ISO_LOCAL_DATE);
        String type = planType != null ? planType : PlanType.NORMAL.name();

        DailyPlanSnapshotDO snapshot = findPlanSnapshot(userId, date, type);
        if (snapshot == null) {
            throw new BusinessException(ErrorCodeEnum.DAILY_PLAN_NOT_FOUND);
        }

        return toResponse(snapshot, planDate);
    }

    private DailyPlanSnapshotDO findPlanSnapshot(Long userId, LocalDate planDate, String planType) {
        LambdaQueryWrapper<DailyPlanSnapshotDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyPlanSnapshotDO::getUserId, userId)
                .eq(DailyPlanSnapshotDO::getPlanDate, planDate)
                .eq(DailyPlanSnapshotDO::getPlanType, planType);
        return planSnapshotMapper.selectOne(wrapper);
    }

    private DailyPlanResponse toResponse(DailyPlanSnapshotDO snapshot, String planDate) {
        List<DailyPlanItemDO> items = planItemMapper.selectList(
                new LambdaQueryWrapper<DailyPlanItemDO>()
                        .eq(DailyPlanItemDO::getDailyPlanSnapshotId, snapshot.getId())
                        .orderByAsc(DailyPlanItemDO::getSequenceNo));

        List<DailyPlanItemResponse> newItems = new ArrayList<>();
        List<DailyPlanItemResponse> reviewItems = new ArrayList<>();

        for (DailyPlanItemDO itemDO : items) {
            LearningItemDO li = learningItemMapper.selectById(itemDO.getLearningItemId());
            if (li != null && !isPresentableLearningItem(li)) {
                continue;
            }
            DailyPlanItemResponse resp = li != null
                    ? toItemResponse(li, itemDO.getItemRole(), itemDO.getRecommendedMode(),
                            itemDO.getPriorityScore(), itemDO.getSelectionReason())
                    : new DailyPlanItemResponse(itemDO.getLearningItemId(), null, null,
                            "", "", null, null, Collections.emptyList(), itemDO.getItemRole(),
                            itemDO.getRecommendedMode(), itemDO.getPriorityScore(), itemDO.getSelectionReason());
            if ("NEW".equals(itemDO.getItemRole())) {
                newItems.add(resp);
            } else {
                reviewItems.add(resp);
            }
        }

        DailyPlanRationaleResponse rationale = new DailyPlanRationaleResponse(
                null, Collections.emptyList(), snapshot.getLoadReason());

        return new DailyPlanResponse(
                snapshot.getPlanCode(), planDate, snapshot.getPlanType(), snapshot.getStatus(),
                newItems, reviewItems, rationale);
    }

    private List<LearningItemDO> selectBoundedActiveCandidates(List<String> preferredThemes) {
        List<LearningItemDO> candidates = selectActiveCandidateWindow(preferredThemes, 500);
        if (candidates.isEmpty() && preferredThemes != null && !preferredThemes.isEmpty()) {
            candidates = selectActiveCandidateWindow(Collections.emptyList(), 500);
        }
        return candidates;
    }

    private List<LearningItemDO> selectActiveCandidateWindow(List<String> preferredThemes, int limit) {
        LambdaQueryWrapper<LearningItemDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningItemDO::getStatus, "ACTIVE");
        if (preferredThemes != null && !preferredThemes.isEmpty()) {
            wrapper.in(LearningItemDO::getTheme, preferredThemes);
        }
        wrapper.orderByAsc(LearningItemDO::getDifficulty, LearningItemDO::getId)
                .last("limit " + limit);

        List<LearningItemDO> raw = learningItemMapper.selectList(wrapper);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return raw.stream()
                .filter(this::isPresentableLearningItem)
                .toList();
    }

    private boolean isPresentableLearningItem(LearningItemDO item) {
        if (item == null) return false;
        String content = item.getContent();
        String meaning = item.getMeaningZh();
        if (content == null || content.isBlank()) return false;
        if (meaning == null || meaning.isBlank()) return false;
        String trimmed = content.trim();
        if (trimmed.startsWith("'")) return false;
        return trimmed.chars().anyMatch(Character::isLetter);
    }

    private boolean hasVisiblePlanItems(DailyPlanResponse response) {
        return response != null
                && ((!response.newItems().isEmpty()) || (!response.reviewItems().isEmpty()));
    }

    private void deletePlanSnapshot(Long snapshotId) {
        planItemMapper.delete(new LambdaQueryWrapper<DailyPlanItemDO>()
                .eq(DailyPlanItemDO::getDailyPlanSnapshotId, snapshotId));
        planSnapshotMapper.deleteById(snapshotId);
    }

    private DailyPlanItemResponse toItemResponse(LearningItemDO item, String itemRole,
                                                 String recommendedMode, BigDecimal priorityScore,
                                                 String selectionReason) {
        return new DailyPlanItemResponse(
                item.getId(), item.getItemCode(), item.getType(), item.getContent(), item.getMeaningZh(),
                item.getDifficulty(), item.getTheme(), fromJsonMapList(item.getExamples()), itemRole,
                recommendedMode, priorityScore, selectionReason);
    }

    private MasterySnapshot toMasterySnapshot(MasteryStateDO ms) {
        return new MasterySnapshot(
                ms.getLearningItemId(), ms.getRecognitionScore(), ms.getOutputScore(),
                ms.getForgetRisk(), ms.getLastSeenAt(), ms.getNextReviewAt(),
                ms.getStatus(), ms.getCorrectCount(), ms.getWrongCount());
    }

    private double calcRecentCompletionRate(Long userId) {
        // Simplified: count completed vs total sessions in last 3 days
        LocalDateTime since = LocalDateTime.now().minusDays(3);
        List<StudySessionDO> sessions = studySessionMapper.selectList(
                new LambdaQueryWrapper<StudySessionDO>()
                        .eq(StudySessionDO::getUserId, userId)
                        .ge(StudySessionDO::getCreatedAt, since));
        if (sessions.isEmpty()) return 0.5; // neutral default
        long completed = sessions.stream().filter(s -> "COMPLETED".equals(s.getStatus())).count();
        return (double) completed / sessions.size();
    }

    private double calcRecentAccuracy(Long userId) {
        // Placeholder: return neutral default; accuracy from attempt_log not wired yet
        return 0.5;
    }

    private long countOverdueItems(Long userId) {
        return masteryStateMapper.selectCount(
                new LambdaQueryWrapper<MasteryStateDO>()
                        .eq(MasteryStateDO::getUserId, userId)
                        .lt(MasteryStateDO::getNextReviewAt, LocalDateTime.now()));
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, String>> fromJsonMapList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
