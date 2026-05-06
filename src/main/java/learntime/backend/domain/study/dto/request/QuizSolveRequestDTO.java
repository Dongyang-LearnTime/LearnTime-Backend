package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "퀴즈 풀이 정보를 담은 DTO")
public record QuizSolveRequestDTO(
        @NotNull(message = "퀴즈 문제 ID는 필수입니다.")
        Long quizQuestionId,

        @NotBlank(message = "사용자 정답은 필수입니다")
        String userAnswer
) { }
