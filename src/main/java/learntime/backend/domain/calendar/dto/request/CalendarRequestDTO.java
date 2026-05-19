package learntime.backend.domain.calendar.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record CalendarRequestDTO (
    @NotBlank(message = "제목은 필수입니다.")
    String title,
    String content,

    @NotNull(message = "날짜는 필수입니다.")
    LocalDateTime targetDate,
    Boolean isCompleted
    ) {}
