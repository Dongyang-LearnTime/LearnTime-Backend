package learntime.backend.domain.calendar.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "캘린더 일정 생성/수정 요청 DTO")
public record CalendarRequestDTO (
    @Schema(description = "일정 상세 내용", example = "백준 3문제 풀이")
    String content,

    @NotNull(message = "날짜는 필수입니다.")
    @Schema(description = "일정 날짜 및 시간")
    LocalDateTime targetDate,

    @Schema(description = "완료 여부", defaultValue = "false")
    Boolean isCompleted,

    @Schema(description = "중요 일정 여부", defaultValue = "false")
    Boolean isImportant
) {}
