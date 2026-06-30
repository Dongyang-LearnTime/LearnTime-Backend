package learntime.backend.domain.study_progress.scheduler;

import learntime.backend.domain.study_progress.service.StudyDailyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class StudyDailyPlanScheduler {

    private final StudyDailyService studyDailyPlanService;

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        try {
            studyDailyPlanService.markIncompletePlansAsFailure();
        } catch (Exception e) {
            LoggerFactory.getLogger(StudyDailyPlanScheduler.class).error("[스케줄러 실패] 서버 시작 시 미완료 진도 정리 실패", e);
        }
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void scheduledTask() {
        try {
            studyDailyPlanService.markIncompletePlansAsFailure();
            studyDailyPlanService.finalizeExpiredStudyMembers();
        } catch (Exception e) {
            LoggerFactory.getLogger(StudyDailyPlanScheduler.class).error("[스케줄러 실패] 정기 스케줄러 진도 정리 실패", e);
        }
    }

}