package learntime.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BackendApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    private learntime.backend.domain.study.repository.StudyRestDayRepository studyRestDayRepository;
    
    @org.springframework.beans.factory.annotation.Autowired
    private learntime.backend.domain.study.repository.StudyRestDateRepository studyRestDateRepository;

    @Test
    void queryStudyRestInfo() {
        System.out.println("=== STUDY REST DAYS ===");
        studyRestDayRepository.findAll().forEach(day -> {
            System.out.println("Study ID: " + day.getStudy().getStudyId() + ", Rest Day: " + day.getDayOfWeek());
        });

        System.out.println("=== STUDY REST DATES ===");
        studyRestDateRepository.findAll().forEach(date -> {
            System.out.println("Study ID: " + date.getStudy().getStudyId() + ", Rest Date: " + date.getRestDate());
        });
    }

}
