package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "스터디 피드백 조회 응답 DTO")
public record StudyFeedbackResponseDTO(
        Long feedbackId,             // 피드백 식별자
        String feedbackTitle,        // 피드백 제목
        String feedbackContent,      // 피드백 내용
        LocalDateTime createdAt      // 피드백 생성 일시
) {
}
