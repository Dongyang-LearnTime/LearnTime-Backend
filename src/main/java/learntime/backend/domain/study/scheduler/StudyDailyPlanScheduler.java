package learntime.backend.domain.study.scheduler;

import learntime.backend.domain.study.service.core.StudyDailyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class StudyDailyPlanScheduler {

    private final StudyDailyService studyDailyPlanService;

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        studyDailyPlanService.markIncompletePlansAsFailure();
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void scheduledTask() {
        studyDailyPlanService.markIncompletePlansAsFailure();
    }
}