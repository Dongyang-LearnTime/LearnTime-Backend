package learntime.backend.domain.user.converter;

import learntime.backend.domain.user.dto.request.SignUpRequestDTO;
import learntime.backend.domain.user.dto.response.MyPageResponseDTO;
import learntime.backend.domain.user.enums.Role;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.model.UserTerms;
import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.BusinessException;

import java.util.stream.Collectors;

public class UserConverter {

    public UserConverter() {
        throw new BusinessException(ErrorCode.UTILITY_CLASS_INSTANTIATION);
    }

    public static MyPageResponseDTO toMyPageResponseDTO(User user) {
        return MyPageResponseDTO.builder()
                .email(user.getEmail())
                .userName(user.getName())
                .point(user.getPoint())
                .socialProvider(user.getSocialProvider().name())
                .termsAgreements(user.getUserTerms().stream()
                        .collect(Collectors.toMap(ut -> ut.getTerms().name(), UserTerms::getAgreed)))
                .createdAt(user.getCreatedAt())
                .role(user.getRole())
                .build();
    }

    public static User toUserEntity(SignUpRequestDTO signUpData, String encodedPassword) {
        return User.builder()
                .name(signUpData.userName())
                .email(signUpData.email())
                .password(encodedPassword)
                .role(Role.ROLE_USER) // 관리자는 ROLE_ADMIN
                .build();
    }
}
