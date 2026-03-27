package learntime.backend.domain.study.dto.request;

import learntime.backend.domain.study.dto.response.StudyPlanResponseDTO;

public record SavePlanRequestDTO (
        GeminiStudyRequestDTO planInfo,
        StudyPlanResponseDTO planGeminiResult
) {}
