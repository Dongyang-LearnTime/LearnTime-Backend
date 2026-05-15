package learntime.backend.domain.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 피드백 응답 DTO")
public record AiFeedbackResponseDTO(
        String feedbackTitle,     // 피드백 제목
        String feedbackContent    // 피드백 상세 내용
) {
}
