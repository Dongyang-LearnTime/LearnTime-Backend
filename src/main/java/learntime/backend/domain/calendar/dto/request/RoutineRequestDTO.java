package learntime.backend.domain.calendar.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Builder
@Schema(description = "루틴 생성/수정 요청 DTO")
public record RoutineRequestDTO(
    @Schema(description = "루틴 상세 내용", example = "매주 알고리즘 문제 풀이 및 깃허브 업로드")
    @Size(max = 200, message = "내용은 200자를 넘을 수 없습니다.")
    String content,

    @NotNull(message = "시작 시각은 필수입니다.")
    @Schema(description = "루틴 시작 시각", example = "10:00:00")
    LocalTime startTime,

    @NotNull(message = "시작일은 필수입니다.")
    @Schema(description = "루틴 시작일", example = "2026-05-25")
    LocalDate startDate,

    @Schema(description = "루틴 종료일 (null 이면 무기한 반복)", example = "2026-12-31")
    LocalDate endDate,

    @Schema(description = "중요 루틴 여부", defaultValue = "false")
    Boolean isImportant,

    @NotEmpty(message = "반복할 요일을 최소 하나 이상 선택해야 합니다.")
    @Schema(description = "반복 요일 목록", example = "[\"MONDAY\", \"WEDNESDAY\"]")
    Set<DayOfWeek> daysOfWeek
) {}
