package learntime.backend.domain.study.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder // 실무 최적화: 가독성 높은 객체 생성을 위해 추가
@NoArgsConstructor
@AllArgsConstructor // Builder 사용 시 기본 생성자와 함께 필수
public class Yes24BookResponseDTO {
    private String title; // 책 이름
    private String author; // 작가
    private String publisher; // 출판사
    private String linkUrl; // 상세 정보 링크
}
