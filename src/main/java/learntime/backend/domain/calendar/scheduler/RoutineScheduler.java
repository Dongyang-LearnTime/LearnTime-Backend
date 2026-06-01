package learntime.backend.domain.calendar.scheduler;

import learntime.backend.domain.calendar.model.CalendarRecord;
import learntime.backend.domain.calendar.model.Routine;
import learntime.backend.domain.calendar.repository.CalendarRecordRepository;
import learntime.backend.domain.calendar.repository.RoutineRepository;
import learntime.backend.domain.calendar.service.RoutineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoutineScheduler {

    private final RoutineRepository routineRepository;
    private final CalendarRecordRepository calendarRecordRepository;
    private final RoutineService routineService;

    private static final int THRESHOLD_DAYS = 14;
    private static final int GENERATION_DAYS = 60;

    // 매일 새벽 4시에 루틴 추가 생산 스케줄러 실행 (서울 시간대 기준)
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void topUpRoutineSchedules() {
        log.info("[루틴 스케줄러] 루틴 추가 생산 작업 시작");
        try {
            LocalDate today = LocalDate.now();
            List<Routine> activeRoutines = routineRepository.findAllActiveRoutines(today);
            log.info("[루틴 스케줄러] 현재 활성화 상태인 루틴 개수: {}개", activeRoutines.size());

            int updatedCount = 0;
            for (Routine routine : activeRoutines) {
                try {
                    boolean isUpdated = routineService.processRoutineForScheduler(routine.getRoutineId(), today);
                    if (isUpdated) {
                        updatedCount++;
                    }
                } catch (Exception e) {
                    log.error("[루틴 스케줄러 실패] 루틴 ID: {} 처리 중 오류 발생", routine.getRoutineId(), e);
                }
            }
            log.info("[루틴 스케줄러] {}개의 루틴에 대해 일정을 추가 생성 완료했습니다.", updatedCount);
        } catch (Exception e) {
            log.error("[루틴 스케줄러 실패] 작업 중 오류 발생", e);
        }
    }
}
