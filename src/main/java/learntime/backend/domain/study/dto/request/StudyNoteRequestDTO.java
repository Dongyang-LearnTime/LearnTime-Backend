package learntime.backend.domain.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record StudyNoteRequestDTO(
        @NotNull(message = "공부 ID는 필수입니다.")
        Long studyId,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,

        @NotBlank(message = "본문을 입력해주세요.")
        @Size(max = 100000, message = "본문 내용이 너무 깁니다.")
        String content
) { }
