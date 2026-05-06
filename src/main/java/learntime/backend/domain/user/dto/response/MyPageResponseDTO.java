package learntime.backend.domain.user.dto.response;

import lombok.Builder;
import learntime.backend.domain.user.model.User;

@Builder
public record MyPageResponseDTO(
        String email,
        String userName,
        Integer point,
        String socialProvider
) {
    public static MyPageResponseDTO from(User user) {
        return MyPageResponseDTO.builder()
                .email(user.getEmail())
                .userName(user.getName()) // Entity의 name 필드
                .point(user.getPoint())
                .socialProvider(user.getSocialProvider().name())
                .build();
    }
}
