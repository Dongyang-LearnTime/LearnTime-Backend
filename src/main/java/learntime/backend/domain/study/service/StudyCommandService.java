package learntime.backend.domain.study.service;

import learntime.backend.domain.study.dto.request.GeminiStudyRequestDTO;
import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;
import learntime.backend.domain.study.model.*;
import learntime.backend.domain.study.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyCommandService {

    private final StudyRepository studyRepository;
    private final StudyDailyPlanRepository studyDailyPlanRepository;
    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;

    @Transactional
    public void saveStudyPlan(GeminiStudyRequestDTO request, StudyPlanResponseDTO geminiResult) {
        Study study = Study.builder()
                .studyTitle(request.studyTitle())
                .bookTitle(request.bookTitle())
                .startDate(request.startDate())
                .endDate(request.endDate())
                // userId 컬럼이 Study 엔티티에 추가되면 여기에 매핑
                .build();

        studyRepository.save(study);

        List<StudyDailyPlan> dailyPlans = geminiResult.dailyPlans().stream()
                .map(planDto -> StudyDailyPlan.builder()
                        .study(study) // 외래키
                        .dayNumber(planDto.day())
                        .planContent(planDto.tasks())
                        .build())
                .toList();

        studyDailyPlanRepository.saveAll(dailyPlans);

        // 쉬는 날이 존재한다면 저장
        if (!CollectionUtils.isEmpty(request.restDates())) {
            List<StudyRestDate> restDates = request.restDates().stream()
                    .map(date -> StudyRestDate.builder()
                            .study(study)
                            .restDate(date)
                            .build())
                    .toList();

            studyRestDateRepository.saveAll(restDates);
        }

        // 쉬는 요일이 존재한다면 저장
        if (!CollectionUtils.isEmpty(request.restDays())) {
            List<StudyRestDay> restDays = request.restDays().stream()
                    .map(dayOfWeek -> StudyRestDay.builder()
                            .study(study)
                            .dayOfWeek(dayOfWeek)
                            .build())
                    .toList();

            studyRestDayRepository.saveAll(restDays);
        }
    }
}
