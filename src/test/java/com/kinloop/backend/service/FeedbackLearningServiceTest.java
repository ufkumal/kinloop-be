package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.feedback.ActivityFeedbackResponse;
import com.kinloop.backend.dto.feedback.SubmitActivityFeedbackRequest;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ChildDomainLevel;
import com.kinloop.backend.entity.ChildIntelligenceScore;
import com.kinloop.backend.entity.ChildProfileSnapshot;
import com.kinloop.backend.entity.DailyPlan;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.DunnProfile;
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.entity.enums.DevelopmentDomain;
import com.kinloop.backend.entity.enums.DunnQuadrant;
import com.kinloop.backend.entity.enums.FeedbackReason;
import com.kinloop.backend.entity.enums.FeedbackType;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementType;
import com.kinloop.backend.entity.enums.PlanSlotType;
import com.kinloop.backend.repository.ChildDomainLevelRepository;
import com.kinloop.backend.repository.ChildIntelligenceScoreRepository;
import com.kinloop.backend.repository.ChildProfileSnapshotRepository;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import com.kinloop.backend.repository.DunnProfileRepository;
import com.kinloop.backend.repository.FeedbackEffectRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import com.kinloop.backend.service.matching.MatchingParameters;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FeedbackLearningServiceTest {
    @Mock private DailyPlanItemRepository dailyPlanItemRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private FeedbackEffectRepository feedbackEffectRepository;
    @Mock private ChildProfileSnapshotRepository profileRepository;
    @Mock private DunnProfileRepository dunnRepository;
    @Mock private ChildIntelligenceScoreRepository intelligenceRepository;
    @Mock private ChildDomainLevelRepository domainRepository;
    @Mock private MatchingParameters matchingParameters;
    @InjectMocks private FeedbackLearningService service;

    private final Child child = new Child();
    private ChildProfileSnapshot profile;
    private ChildIntelligenceScore target;
    private ChildIntelligenceScore secondary;
    private ChildDomainLevel domainLevel;
    private DailyPlanItem item;

    @BeforeEach
    void setUp() {
        child.setId(7L);
        Activity activity = activity(2, InvolvementType.BIRLIKTE);
        DailyPlan plan = new DailyPlan(7L, LocalDate.now());
        plan.add(activity, PlanSlotType.DEVELOP, BigDecimal.TEN);
        item = plan.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", 11L);

        profile = new ChildProfileSnapshot();
        profile.setDunnQuadrant(DunnQuadrant.C1);
        profile.setSeparationAnxiety(2);
        target = new ChildIntelligenceScore(7L, IntelligenceType.VERBAL_LINGUISTIC,
                new BigDecimal("3.00"));
        secondary = new ChildIntelligenceScore(7L, IntelligenceType.MUSICAL,
                new BigDecimal("3.00"));
        domainLevel = new ChildDomainLevel(7L, DevelopmentDomain.LANGUAGE, (short) 1);

        when(dailyPlanItemRepository.findByIdAndDailyPlanChildId(11L, 7L)).thenReturn(Optional.of(item));
        when(feedbackRepository.existsByDailyPlanItemId(11L)).thenReturn(false);
        when(feedbackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingParameters.load()).thenReturn(parameters());
        when(profileRepository.findByChildIdAndCurrentTrue(7L)).thenReturn(Optional.of(profile));
        when(intelligenceRepository.findByChildId(7L)).thenReturn(List.of(target, secondary));
        when(domainRepository.findByChildId(7L)).thenReturn(List.of(domainLevel));
    }

    @Test
    void likedUpdatesGardnerLedgersAndDifficultySensitiveStreakIndependently() {
        ActivityFeedbackResponse response = service.submit(
                child, 11L, new SubmitActivityFeedbackRequest(FeedbackType.LIKED, null));

        assertEquals(0, new BigDecimal("3.30").compareTo(target.getScore()));
        assertEquals(0, new BigDecimal("3.15").compareTo(secondary.getScore()));
        assertEquals(1, target.getFeedbackCount());
        assertEquals(0, secondary.getFeedbackCount());
        assertEquals(0, new BigDecimal("1.0").compareTo(domainLevel.getStreak()));
        assertEquals((short) 1, response.domainLevel());
        assertTrue(item.isCompleted());
        verify(feedbackEffectRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void struggledLeavesGardnerScoresUntouchedAndPenalizesOnlyTheDomainStreak() {
        service.submit(child, 11L, new SubmitActivityFeedbackRequest(FeedbackType.STRUGGLED, null));

        assertEquals(0, new BigDecimal("3.00").compareTo(target.getScore()));
        assertEquals(1, target.getFeedbackCount());
        assertEquals(0, new BigDecimal("-0.5").compareTo(domainLevel.getStreak()));
        verify(feedbackEffectRepository, never()).save(any());
    }

    @Test
    void dislikedInterestReducesOnlyTargetGardnerScore() {
        ActivityFeedbackResponse response = service.submit(
                child, 11L, new SubmitActivityFeedbackRequest(FeedbackType.DISLIKED, null));

        assertEquals(FeedbackReason.INTEREST, response.resolvedReason());
        assertEquals(0, new BigDecimal("2.85").compareTo(target.getScore()));
        assertEquals(0, new BigDecimal("3.00").compareTo(secondary.getScore()));
        assertEquals(0, BigDecimal.ZERO.compareTo(domainLevel.getStreak()));
    }

    @Test
    void sensoryDislikeDoesNotChangeGardnerOrDomainLedgers() {
        profile.setDunnQuadrant(DunnQuadrant.C3);
        DunnProfile dunn = new DunnProfile();
        ReflectionTestUtils.setField(dunn, "noiseTolerance", (short) 1);
        ReflectionTestUtils.setField(dunn, "visualTolerance", (short) 3);
        ReflectionTestUtils.setField(dunn, "movementTolerance", (short) 3);
        when(dunnRepository.findById(DunnQuadrant.C3)).thenReturn(Optional.of(dunn));

        ActivityFeedbackResponse response = service.submit(
                child, 11L, new SubmitActivityFeedbackRequest(FeedbackType.DISLIKED, null));

        assertEquals(FeedbackReason.SENSORY, response.resolvedReason());
        assertEquals(0, new BigDecimal("3.00").compareTo(target.getScore()));
        assertEquals(0, BigDecimal.ZERO.compareTo(domainLevel.getStreak()));
        verify(feedbackEffectRepository, never()).save(any());
    }

    @Test
    void trimsFreeTextBeforeSavingFeedback() {
        service.submit(child, 11L,
                new SubmitActivityFeedbackRequest(FeedbackType.STRUGGLED, "  Needed a little help.  "));

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertEquals("Needed a little help.", captor.getValue().getFreeText());
    }

    @Test
    void storesBlankFreeTextAsNull() {
        service.submit(child, 11L,
                new SubmitActivityFeedbackRequest(FeedbackType.STRUGGLED, "  \t\n  "));

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertNull(captor.getValue().getFreeText());
    }

    private Activity activity(int difficulty, InvolvementType involvementType) {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", 5L);
        ReflectionTestUtils.setField(activity, "targetDomain", DevelopmentDomain.LANGUAGE);
        ReflectionTestUtils.setField(activity, "targetIntelligence", IntelligenceType.VERBAL_LINGUISTIC);
        ReflectionTestUtils.setField(activity, "secondaryIntelligence", IntelligenceType.MUSICAL);
        ReflectionTestUtils.setField(activity, "difficulty", (short) difficulty);
        ReflectionTestUtils.setField(activity, "noiseLoad", (short) 2);
        ReflectionTestUtils.setField(activity, "visualLoad", (short) 2);
        ReflectionTestUtils.setField(activity, "physicalIntensity", (short) 2);
        ReflectionTestUtils.setField(activity, "involvementType", involvementType);
        return activity;
    }

    private Map<String, BigDecimal> parameters() {
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("liked_target_delta", new BigDecimal("0.30"));
        values.put("liked_secondary_delta", new BigDecimal("0.15"));
        values.put("disliked_interest_delta", new BigDecimal("-0.15"));
        values.put("gardner_runtime_min_score", BigDecimal.ZERO);
        values.put("gardner_runtime_max_score", new BigDecimal("5.00"));
        values.put("level_credit_stretch", new BigDecimal("1.0"));
        values.put("level_credit_at_level", new BigDecimal("0.5"));
        values.put("level_credit_below", BigDecimal.ZERO);
        values.put("level_penalty_struggle_stretch", new BigDecimal("-0.5"));
        values.put("level_penalty_struggle_at_level", new BigDecimal("-1.0"));
        values.put("level_up_threshold", new BigDecimal("3.0"));
        values.put("level_down_threshold", new BigDecimal("-1.0"));
        values.put("level_min", BigDecimal.ONE);
        values.put("level_max", new BigDecimal("4"));
        values.put("ceiling_counter_cap", new BigDecimal("1.0"));
        values.put("attachment_anxiety_threshold", new BigDecimal("4"));
        return values;
    }
}
