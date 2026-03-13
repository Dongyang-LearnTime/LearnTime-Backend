package learntime.backend.domain.user.dto.response;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;
}