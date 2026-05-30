package learntime.backend.domain.badge.event;

import learntime.backend.domain.badge.enums.BadgeType;
import learntime.backend.domain.badge.enums.StatKey;
import learntime.backend.domain.badge.model.UserActivityStat;
import learntime.backend.domain.badge.model.UserBadge;
import learntime.backend.domain.badge.repository.UserActivityStatRepository;
import learntime.backend.domain.badge.repository.UserBadgeRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeEventListenerTest {

    @InjectMocks
    private BadgeEventListener badgeEventListener;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserActivityStatRepository statRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@test.com")
                .name("테스터")
                .build();
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("userId");
            field.setAccessible(true);
            field.set(user, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("최초로 공부 일정 완료 시 통계가 1로 시작하고 '앞으로 한 걸음' 배지가 부여된다")
    void handleStudyCompleted_firstTime() {
        // given
        // KST 기준 2026-05-25 09:00:00 (오전 8시 이후이므로 미라클 모닝은 아님)
        LocalDateTime completedAtUTC = LocalDateTime.of(2026, 5, 25, 0, 0, 0); 
        StudyCompletedEvent event = new StudyCompletedEvent(1L, completedAtUTC);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(new ArrayList<>());
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(new ArrayList<>());

        // when
        badgeEventListener.handleStudyCompleted(event);

        // then
        // 1. 통계 저장 확인
        ArgumentCaptor<List<UserActivityStat>> statsCaptor = ArgumentCaptor.forClass(List.class);
        verify(statRepository, times(1)).saveAll(statsCaptor.capture());
        List<UserActivityStat> savedStats = statsCaptor.getValue();
        
        UserActivityStat studyStat = savedStats.stream()
                .filter(s -> s.getStatKey() == StatKey.CONSECUTIVE_STUDY_DAYS)
                .findFirst().orElseThrow();
        assertThat(studyStat.getStatValue()).isEqualTo(1L);
        assertThat(studyStat.getLastActionDate()).isEqualTo(LocalDate.of(2026, 5, 25));

        // 2. 배지 획득 확인 (앞으로 한 걸음)
        ArgumentCaptor<UserBadge> badgeCaptor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository, times(1)).save(badgeCaptor.capture());
        assertThat(badgeCaptor.getValue().getBadgeType()).isEqualTo(BadgeType.FIRST_STEP);
    }

    @Test
    @DisplayName("어제 공부 완료를 했고 오늘 완료하면 연속 공부 일수가 1 증가한다")
    void handleStudyCompleted_consecutive() {
        // given
        LocalDateTime completedAtUTC = LocalDateTime.of(2026, 5, 25, 0, 0, 0); // KST 2026-05-25 09:00:00
        StudyCompletedEvent event = new StudyCompletedEvent(1L, completedAtUTC);

        UserActivityStat existingStat = UserActivityStat.builder()
                .user(user)
                .statKey(StatKey.CONSECUTIVE_STUDY_DAYS)
                .build();
        existingStat.resetValueToOne(); // value = 1
        existingStat.updateLastActionDate(LocalDate.of(2026, 5, 24)); // 어제 날짜

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(List.of(existingStat));
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(List.of(BadgeType.FIRST_STEP));

        // when
        badgeEventListener.handleStudyCompleted(event);

        // then
        assertThat(existingStat.getStatValue()).isEqualTo(2L);
        assertThat(existingStat.getLastActionDate()).isEqualTo(LocalDate.of(2026, 5, 25));
        // 이미 배지가 있으므로 새로 추가 저장되지 않음
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("하루 이상 빼먹은 후 완료하면 연속 공부 일수가 1로 리셋된다")
    void handleStudyCompleted_resetToOne() {
        // given
        LocalDateTime completedAtUTC = LocalDateTime.of(2026, 5, 25, 0, 0, 0); // KST 2026-05-25 09:00:00
        StudyCompletedEvent event = new StudyCompletedEvent(1L, completedAtUTC);

        UserActivityStat existingStat = UserActivityStat.builder()
                .user(user)
                .statKey(StatKey.CONSECUTIVE_STUDY_DAYS)
                .build();
        // 5일 연속이었으나 어제(24일) 건너뛰고 그저께(23일) 마지막 액션
        for (int i = 0; i < 5; i++) {
            existingStat.incrementValue();
        }
        existingStat.updateLastActionDate(LocalDate.of(2026, 5, 23));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(List.of(existingStat));
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(List.of(BadgeType.FIRST_STEP));

        // when
        badgeEventListener.handleStudyCompleted(event);

        // then
        assertThat(existingStat.getStatValue()).isEqualTo(1L);
        assertThat(existingStat.getLastActionDate()).isEqualTo(LocalDate.of(2026, 5, 25));
    }

    @Test
    @DisplayName("오늘 이미 공부 완료를 기록했다면 멱등성이 보장되어 값이 증가하지 않는다")
    void handleStudyCompleted_idempotent() {
        // given
        LocalDateTime completedAtUTC = LocalDateTime.of(2026, 5, 25, 0, 0, 0); // KST 2026-05-25 09:00:00
        StudyCompletedEvent event = new StudyCompletedEvent(1L, completedAtUTC);

        UserActivityStat existingStat = UserActivityStat.builder()
                .user(user)
                .statKey(StatKey.CONSECUTIVE_STUDY_DAYS)
                .build();
        existingStat.resetValueToOne();
        existingStat.updateLastActionDate(LocalDate.of(2026, 5, 25)); // 오늘 이미 완료함

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(List.of(existingStat));
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(List.of(BadgeType.FIRST_STEP));

        // when
        badgeEventListener.handleStudyCompleted(event);

        // then
        assertThat(existingStat.getStatValue()).isEqualTo(1L); // 증가하지 않고 유지
        assertThat(existingStat.getLastActionDate()).isEqualTo(LocalDate.of(2026, 5, 25));
    }

    @Test
    @DisplayName("오전 8시 이전에 공부를 완료하면 미라클 모닝 통계도 같이 업데이트된다")
    void handleStudyCompleted_miracleMorning() {
        // given
        // KST 기준 2026-05-25 07:00:00 (오전 8시 전)
        LocalDateTime completedAtUTC = LocalDateTime.of(2026, 5, 24, 22, 0, 0);
        StudyCompletedEvent event = new StudyCompletedEvent(1L, completedAtUTC);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(new ArrayList<>());
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(new ArrayList<>());

        // when
        badgeEventListener.handleStudyCompleted(event);

        // then
        ArgumentCaptor<List<UserActivityStat>> statsCaptor = ArgumentCaptor.forClass(List.class);
        verify(statRepository).saveAll(statsCaptor.capture());
        List<UserActivityStat> savedStats = statsCaptor.getValue();

        UserActivityStat studyStat = savedStats.stream()
                .filter(s -> s.getStatKey() == StatKey.CONSECUTIVE_STUDY_DAYS)
                .findFirst().orElseThrow();
        UserActivityStat morningStat = savedStats.stream()
                .filter(s -> s.getStatKey() == StatKey.CONSECUTIVE_MIRACLE_MORNING)
                .findFirst().orElseThrow();

        assertThat(studyStat.getStatValue()).isEqualTo(1L);
        assertThat(morningStat.getStatValue()).isEqualTo(1L);
        assertThat(morningStat.getLastActionDate()).isEqualTo(LocalDate.of(2026, 5, 25));

        // 배지 획득 검증 (앞으로 한 걸음 + 일찍 일어나는 새가 벌레를)
        verify(userBadgeRepository, times(2)).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("퀴즈 10회 연속 만점 시 '인간 GPT' 배지를 획득한다")
    void handleQuizCompleted_perfectScore_awardsBadge() {
        // given
        QuizCompletedEvent event = new QuizCompletedEvent(1L, true, LocalDateTime.now());

        UserActivityStat existingStat = UserActivityStat.builder()
                .user(user)
                .statKey(StatKey.CONSECUTIVE_PERFECT_QUIZ)
                .build();
        // 이미 9회 연속 만점 상태
        for (int i = 0; i < 9; i++) {
            existingStat.incrementValue();
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(List.of(existingStat));
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(new ArrayList<>());

        // when
        badgeEventListener.handleQuizCompleted(event);

        // then
        assertThat(existingStat.getStatValue()).isEqualTo(10L);
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("퀴즈 만점을 실패하면 연속 만점 통계가 0으로 리셋되고 배지가 부여되지 않는다")
    void handleQuizCompleted_failedScore_resetsStat() {
        // given
        QuizCompletedEvent event = new QuizCompletedEvent(1L, false, LocalDateTime.now());

        UserActivityStat existingStat = UserActivityStat.builder()
                .user(user)
                .statKey(StatKey.CONSECUTIVE_PERFECT_QUIZ)
                .build();
        existingStat.incrementValue(); // 1회 만점이었던 상태

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(List.of(existingStat));
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(new ArrayList<>());

        // when
        badgeEventListener.handleQuizCompleted(event);

        // then
        assertThat(existingStat.getStatValue()).isEqualTo(0L);
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("필기 80번 이상 업로드 시 '팔십대장경' 배지를 획득한다")
    void handleNoteUploaded_awardsBadge() {
        // given
        NoteUploadedEvent event = new NoteUploadedEvent(1L, LocalDateTime.now());

        UserActivityStat existingStat = UserActivityStat.builder()
                .user(user)
                .statKey(StatKey.TOTAL_NOTE_COUNT)
                .build();
        // 이미 79번 업로드한 상태
        for (int i = 0; i < 79; i++) {
            existingStat.incrementValue();
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(List.of(existingStat));
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(new ArrayList<>());

        // when
        badgeEventListener.handleNoteUploaded(event);

        // then
        assertThat(existingStat.getStatValue()).isEqualTo(80L);
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("하루 첫 운동 시 10p가 지급되고 연속 운동 일수가 1 증가한다 (미라클 모닝도 포함)")
    void handleExerciseCompleted_firstTime_awardsPoint() {
        // given: 오전 7시 완료 (오전 8시 전)
        LocalDateTime completedAtUTC = LocalDateTime.of(2026, 5, 24, 22, 0, 0); // KST 2026-05-25 07:00:00
        ExerciseCompletedEvent event = new ExerciseCompletedEvent(1L, completedAtUTC);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(new ArrayList<>());
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(new ArrayList<>());

        // when
        badgeEventListener.handleExerciseCompleted(event);

        // then
        // 통계 저장 확인 (운동 연속 + 미라클 모닝)
        ArgumentCaptor<List<UserActivityStat>> statsCaptor = ArgumentCaptor.forClass(List.class);
        verify(statRepository, times(1)).saveAll(statsCaptor.capture());
        
        List<UserActivityStat> savedStats = statsCaptor.getValue();
        UserActivityStat exerciseStat = savedStats.stream()
                .filter(s -> s.getStatKey() == StatKey.CONSECUTIVE_EXERCISE_DAYS)
                .findFirst().orElseThrow();
        assertThat(exerciseStat.getStatValue()).isEqualTo(1L);

        // 일일 포인트 이벤트 발행 확인
        ArgumentCaptor<learntime.backend.domain.point.dto.PointEventDTO> pointCaptor = ArgumentCaptor.forClass(learntime.backend.domain.point.dto.PointEventDTO.class);
        verify(eventPublisher, times(1)).publishEvent(pointCaptor.capture());
        assertThat(pointCaptor.getValue().amount()).isEqualTo(10);
    }

    @Test
    @DisplayName("같은 날 여러 번 운동해도 일일 포인트는 한 번만 지급된다")
    void handleExerciseCompleted_idempotent_noExtraPoint() {
        // given: 오전 9시 완료
        LocalDateTime completedAtUTC = LocalDateTime.of(2026, 5, 25, 0, 0, 0); // KST 2026-05-25 09:00:00
        ExerciseCompletedEvent event = new ExerciseCompletedEvent(1L, completedAtUTC);

        UserActivityStat existingStat = UserActivityStat.builder()
                .user(user)
                .statKey(StatKey.CONSECUTIVE_EXERCISE_DAYS)
                .build();
        existingStat.resetValueToOne();
        existingStat.updateLastActionDate(LocalDate.of(2026, 5, 25)); // 오늘 이미 운동 완료

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(List.of(existingStat));
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(new ArrayList<>());

        // when
        badgeEventListener.handleExerciseCompleted(event);

        // then
        assertThat(existingStat.getStatValue()).isEqualTo(1L); // 증가하지 않음
        // 포인트 이벤트가 더 이상 발생하지 않아야 함
        verify(eventPublisher, never()).publishEvent(any(learntime.backend.domain.point.dto.PointEventDTO.class));
    }

    @Test
    @DisplayName("3일 연속 운동 시 일일 포인트 10p와 보너스 50p가 함께 지급된다")
    void handleExerciseCompleted_consecutive3Days_awardsBonus() {
        // given: 오전 9시 완료
        LocalDateTime completedAtUTC = LocalDateTime.of(2026, 5, 25, 0, 0, 0); // KST 2026-05-25 09:00:00
        ExerciseCompletedEvent event = new ExerciseCompletedEvent(1L, completedAtUTC);

        UserActivityStat existingStat = UserActivityStat.builder()
                .user(user)
                .statKey(StatKey.CONSECUTIVE_EXERCISE_DAYS)
                .build();
        existingStat.incrementValue();
        existingStat.incrementValue(); // 2일 연속 상태
        existingStat.updateLastActionDate(LocalDate.of(2026, 5, 24)); // 마지막이 어제

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(statRepository.findAllByUser_UserId(1L)).thenReturn(List.of(existingStat));
        when(userBadgeRepository.findBadgeTypesByUserId(1L)).thenReturn(new ArrayList<>());

        // when
        badgeEventListener.handleExerciseCompleted(event);

        // then
        assertThat(existingStat.getStatValue()).isEqualTo(3L);

        // 일일 포인트(10p)와 보너스 포인트(50p) 총 2개의 이벤트 발행 확인
        ArgumentCaptor<learntime.backend.domain.point.dto.PointEventDTO> pointCaptor = ArgumentCaptor.forClass(learntime.backend.domain.point.dto.PointEventDTO.class);
        verify(eventPublisher, times(2)).publishEvent(pointCaptor.capture());
        
        List<learntime.backend.domain.point.dto.PointEventDTO> publishedEvents = pointCaptor.getAllValues();
        assertThat(publishedEvents).extracting("amount").containsExactlyInAnyOrder(10, 50);
    }

    @Test
    @DisplayName("존재하지 않는 유저에 대해서는 이벤트를 처리하지 않고 즉시 종료된다")
    void handleEvent_userNotFound() {
        // given
        StudyCompletedEvent event = new StudyCompletedEvent(999L, LocalDateTime.now());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        badgeEventListener.handleStudyCompleted(event);

        // then
        verify(statRepository, never()).findAllByUser_UserId(anyLong());
        verify(statRepository, never()).saveAll(anyList());
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }
}
