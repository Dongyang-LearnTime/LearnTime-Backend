package learntime.backend.domain.user.dto.response;

public record TokenResponseDTO (
    String accessToken,
    String tokenType
) {}