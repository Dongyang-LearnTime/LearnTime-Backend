package learntime.backend.domain.study.dto;

import lombok.Getter;
import lombok.Setter;

// 프론트엔드에서 받는 요청 변수
@Getter @Setter
public class StudyRequestDTO {
    private String bookTitle;
    private String period; // "1주일", "1달" 등
}
