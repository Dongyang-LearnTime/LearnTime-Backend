package learntime.backend.domain.study.service.db;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyRestDate;
import learntime.backend.domain.study.model.StudyRestDay;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StudyRestScheduleManager {

    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveRestDates(Study study, List<LocalDate> restDates) {
        if (CollectionUtils.isEmpty(restDates)) {
            return; // 쉬는 요일이 없는 경우 종료
        }

        List<StudyRestDate> studyRestDates = restDates.stream()
                .map(date -> StudyRestDate.builder()
                        .study(study)
                        .restDate(date)
                        .build())
                .toList();

        studyRestDateRepository.saveAll(studyRestDates);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void saveRestDays(Study study, List<DayOfWeek> restDays) {
        if (CollectionUtils.isEmpty(restDays)) {
            return; // 쉬는 날짜가 없는 경우 종료
        }

        List<StudyRestDay> studyRestDays = restDays.stream()
                .map(dayOfWeek -> StudyRestDay.builder()
                        .study(study)
                        .dayOfWeek(dayOfWeek)
                        .build())
                .toList();

        studyRestDayRepository.saveAll(studyRestDays);
    }
}
