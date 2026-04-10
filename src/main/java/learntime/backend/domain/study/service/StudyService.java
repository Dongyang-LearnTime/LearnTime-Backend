package learntime.backend.domain.study.service;

import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;

    public void deleteStudy(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("공부 진도를 찾을 수 없습니다."));

        studyRepository.hardDeleteById(study.getStudyId());
    }
}
