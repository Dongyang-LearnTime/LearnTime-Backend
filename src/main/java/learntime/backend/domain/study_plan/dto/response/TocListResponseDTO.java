package learntime.backend.domain.study_plan.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "목차 정보 DTO")
public record TocListResponseDTO(
        String chapter,
        String title,
        Integer page
){ }
