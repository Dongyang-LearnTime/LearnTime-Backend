package learntime.backend.domain.study.dto.response;

public record TocListResponseDTO(
        String chapter,
        String title,
        Integer page
){ }
