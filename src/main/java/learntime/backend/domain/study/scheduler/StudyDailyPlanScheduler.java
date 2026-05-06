package learntime.backend.domain.study.scheduler;

import learntime.backend.domain.study.repository.StudyDailyPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyDailyPlanScheduler {

    private final StudyDailyPlanRepository studyDailyPlanRepository;

    // 매일 05시마다 시작 전, 진행 중인 일정 실패로 전환
    @Transactional
    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void markIncompletePlansAsFailure() {
        LocalDate today = LocalDate.now();

        log.info("[Scheduler Start] 미완료 스터디 계획 실패 처리 시작 - 기준일: {}", today);
        long startTime = System.currentTimeMillis();

        // 벌크 업데이트 실행: 반환값은 업데이트된 레코드 수(Time Complexity: DB B-Tree 인덱스 스캔 O(log N) + M)
        int updatedCount = studyDailyPlanRepository.bulkFailIncompletePlans(today);

        long endTime = System.currentTimeMillis();
        log.info("[Scheduler End] 실패 처리 완료 건수: {}건, 소요 시간: {}ms", updatedCount, (endTime - startTime));
    }
}
