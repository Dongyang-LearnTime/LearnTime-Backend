package learntime.backend.domain.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "제목 수정 요청 정보를 담은 DTO")
@Builder
public record UpdateQuizTitleRequestDTO (
    @NotNull(message = "ID는 필수입니다.")
    Long studyQuizId,

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    String quizTitle
) {}
