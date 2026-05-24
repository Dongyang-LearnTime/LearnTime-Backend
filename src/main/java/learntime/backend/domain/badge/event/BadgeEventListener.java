package learntime.backend.domain.badge.event;

import learntime.backend.domain.badge.enums.BadgeType;
import learntime.backend.domain.badge.enums.StatKey;
import learntime.backend.domain.badge.model.UserActivityStat;
import learntime.backend.domain.badge.model.UserBadge;
import learntime.backend.domain.badge.model.UserStats;
import learntime.backend.domain.badge.repository.UserActivityStatRepository;
import learntime.backend.domain.badge.repository.UserBadgeRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeEventListener {

    private final UserRepository userRepository;
    private final UserActivityStatRepository statRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    // 매일 일정 완료 시 배지 검증 (연속 일정 완료 & 미라클 모닝 체크)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStudyCompleted(StudyCompletedEvent event) {
        // 완료 시간 KST 기준 파악
        ZonedDateTime kstTime = event.completedAt().atZone(ZoneId.of("UTC")).withZoneSameInstant(KST_ZONE);
        LocalDate todayKST = kstTime.toLocalDate();
        LocalTime timeKST = kstTime.toLocalTime();

        // 헬퍼 메서드로 유저 로드 후 통계 갱신 로직 수행
        processBadgeEvent(event.userId(), userStats -> {
            // 연속 일정 완료 통계 갱신
            updateConsecutiveStat(userStats, StatKey.CONSECUTIVE_STUDY_DAYS, todayKST);

            // 미라클 모닝 체크 (오전 8시 전)
            if (timeKST.isBefore(LocalTime.of(8, 0))) {
                updateConsecutiveStat(userStats, StatKey.CONSECUTIVE_MIRACLE_MORNING, todayKST);
            }
        });
    }

    // 퀴즈 채점 완료 시 배지 검증 (연속 만점인지 확인)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleQuizCompleted(QuizCompletedEvent event) {
        processBadgeEvent(event.userId(), userStats -> {
            UserActivityStat stat = userStats.getStat(StatKey.CONSECUTIVE_PERFECT_QUIZ);
            
            // 만점이면 값 증가, 아니면 0으로 리셋
            if (event.isPerfect()) {
                stat.incrementValue();
            } else {
                stat.resetValueToZero();
            }
            stat.updateLastActionDate(LocalDate.now(KST_ZONE));
        });
    }
    
    // 필기(학습 내용) 업로드 시 배지 검증 (총 업로드 수 체크)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNoteUploaded(NoteUploadedEvent event) {
        processBadgeEvent(event.userId(), userStats -> {
            UserActivityStat stat = userStats.getStat(StatKey.TOTAL_NOTE_COUNT);
            // 단순 누적이므로 계속 1씩 더해줌
            stat.incrementValue();
            stat.updateLastActionDate(LocalDate.now(KST_ZONE));
        });
    }
    
    // 운동 기록 저장 완료 시 배지 검증 (미라클 모닝 체크)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleExerciseCompleted(ExerciseCompletedEvent event) {
        ZonedDateTime kstTime = event.completedAt().atZone(ZoneId.of("UTC")).withZoneSameInstant(KST_ZONE);
        LocalDate todayKST = kstTime.toLocalDate();
        LocalTime timeKST = kstTime.toLocalTime();
        
        // 오전 8시 전일 때만 통계 갱신 및 검증 수행
        if (timeKST.isBefore(LocalTime.of(8, 0))) {
            processBadgeEvent(event.userId(), userStats -> {
                updateConsecutiveStat(userStats, StatKey.CONSECUTIVE_MIRACLE_MORNING, todayKST);
            });
        }
    }

    // 중복되는 유저 및 통계 로드, 저장, 배지 획득 검증을 통합 처리하는 헬퍼 메서드
    private void processBadgeEvent(Long userId, Consumer<UserStats> statsUpdater) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        List<UserActivityStat> statList = statRepository.findAllByUser_UserId(userId);
        UserStats userStats = new UserStats(user, statList);

        // 각각의 이벤트별 통계 갱신 로직 실행
        statsUpdater.accept(userStats);

        // 변경된 통계 정보 일괄 저장
        statRepository.saveAll(userStats.getAllStats());

        // 배지 조건을 검증하고 조건 만족 시 새 배지 부여
        evaluateAndAwardBadges(user, userStats);
    }

    // 날짜를 비교해 매일매일 빠짐없이 수행했는지 연속성을 업데이트하는 로직
    private void updateConsecutiveStat(UserStats userStats, StatKey key, LocalDate today) {
        UserActivityStat stat = userStats.getStat(key);

        if (stat.getLastActionDate() == null || stat.getLastActionDate().isEqual(today.minusDays(1))) {
            // 처음 달성했거나 어제 달성해서 연속성이 유지되는 경우
            stat.incrementValue();
        } else if (stat.getLastActionDate().isBefore(today.minusDays(1))) {
            // 어제 달성 안 해서 하루라도 빼먹은 경우 1로 리셋
            stat.resetValueToOne();
        }

        // 이미 오늘 달성했다면 추가 증가 없이 날짜만 기록 갱신 (멱등성 보장)
        stat.updateLastActionDate(today);
    }

    // 현재 통계를 바탕으로 아직 없는 배지가 만족되었는지 확인 후 획득 처리
    private void evaluateAndAwardBadges(User user, UserStats userStats) {
        List<BadgeType> ownedBadges = userBadgeRepository.findBadgeTypesByUserId(user.getUserId());

        for (BadgeType badgeType : BadgeType.values()) {
            if (!ownedBadges.contains(badgeType) && badgeType.isSatisfiedBy(userStats)) {
                userBadgeRepository.save(new UserBadge(user, badgeType));
                log.info("[배지 획득] 유저 {}가 '{}' 배지를 획득했습니다.", user.getUserId(), badgeType.getDisplayName());
            }
        }
    }
}
