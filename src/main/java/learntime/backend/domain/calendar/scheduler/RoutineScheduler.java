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
    @Transactional
    public void topUpRoutineSchedules() {
        log.info("[루틴 스케줄러] 루틴 추가 생산 작업 시작");
        try {
            LocalDate today = LocalDate.now();
            List<Routine> activeRoutines = routineRepository.findAllActiveRoutines(today);
            log.info("[루틴 스케줄러] 현재 활성화 상태인 루틴 개수: {}개", activeRoutines.size());

            int updatedCount = 0;
            for (Routine routine : activeRoutines) {
                // 이 루틴으로 생성된 가장 마지막 일정을 조회
                Optional<CalendarRecord> latestRecordOpt = calendarRecordRepository
                        .findFirstByRoutineOrderByTargetDateDesc(routine);

                if (latestRecordOpt.isPresent()) {
                    LocalDate latestDate = latestRecordOpt.get().getTargetDate().toLocalDate();
                    LocalDate thresholdDate = today.plusDays(THRESHOLD_DAYS);

                    // 마지막 일정이 임계일(14일 뒤)보다 이전이거나 같으면 추가로 60일치 생성
                    if (!latestDate.isAfter(thresholdDate)) {
                        LocalDate startFrom = latestDate.plusDays(1);
                        routineService.generateCalendarRecordsForRoutine(routine, startFrom, GENERATION_DAYS);
                        updatedCount++;
                    }
                } else {
                    // 혹시 생성된 일정이 하나도 없는 상태라면 오늘(또는 시작일)부터 60일치 생성
                    LocalDate startFrom = routine.getStartDate().isAfter(today) ? routine.getStartDate() : today;
                    routineService.generateCalendarRecordsForRoutine(routine, startFrom, GENERATION_DAYS);
                    updatedCount++;
                }
            }
            log.info("[루틴 스케줄러] {}개의 루틴에 대해 일정을 추가 생성 완료했습니다.", updatedCount);
        } catch (Exception e) {
            log.error("[루틴 스케줄러 실패] 작업 중 오류 발생", e);
        }
    }
}
