package learntime.backend.domain.exercise.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record WeeklyWeightStatsResponseDTO (
        @Schema(description = "날짜")
        LocalDate date,

        @Schema(description = "일별 총 무게")
        Double dailyTotalWeight
) { }
