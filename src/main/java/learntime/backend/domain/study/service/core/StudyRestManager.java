package learntime.backend.domain.study.service.core;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.model.StudyRestDate;
import learntime.backend.domain.study.model.StudyRestDay;
import learntime.backend.domain.study.converter.StudyConverter;
import learntime.backend.domain.study.repository.StudyRestDateRepository;
import learntime.backend.domain.study.repository.StudyRestDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

// 학습 휴무 일정 관리 전담 서비스
@Service
@RequiredArgsConstructor
public class StudyRestManager {

    private final StudyRestDateRepository studyRestDateRepository;
    private final StudyRestDayRepository studyRestDayRepository;

    // 스터디의 휴무 날짜(특정 날짜) 정보를 저장합니다.
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveRestDates(Study study, List<LocalDate> restDates) {
        if (CollectionUtils.isEmpty(restDates)) {
            return;
        }

        List<StudyRestDate> studyRestDates = restDates.stream()
                .map(date -> StudyConverter.toStudyRestDateEntity(study, date))
                .toList();

        studyRestDateRepository.saveAll(studyRestDates);
    }


    // 스터디의 정기 휴무 요일 정보를 저장합니다.
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveRestDays(Study study, List<DayOfWeek> restDays) {
        if (CollectionUtils.isEmpty(restDays)) {
            return;
        }

        List<StudyRestDay> studyRestDays = restDays.stream()
                .map(dayOfWeek -> StudyConverter.toStudyRestDayEntity(study, dayOfWeek))
                .toList();

        studyRestDayRepository.saveAll(studyRestDays);
    }
}
