package learntime.backend.domain.user.service;

import learntime.backend.domain.user.dto.request.UpdateTermsRequestDTO;
import learntime.backend.domain.user.enums.Terms;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.model.UserTerms;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.domain.user.repository.UserTermsRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MyPageTermsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTermsRepository userTermsRepository;

    @InjectMocks
    private MyPageService myPageService;

    @Test
    @DisplayName("필수 약관을 false로 변경 시도 시 예외가 발생한다")
    void updateTermsAgreement_RequiredTermFalse_ThrowsException() {
        UpdateTermsRequestDTO request = new UpdateTermsRequestDTO(Terms.SERVICE_USE, false);

        assertThatThrownBy(() -> myPageService.updateTermsAgreement(1L, request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(AuthErrorCode.TERMS_NOT_AGREED.getMessage());
    }

    @Test
    @DisplayName("기존에 약관 동의 내역이 존재하는 경우 동의 여부가 업데이트된다")
    void updateTermsAgreement_ExistingTerms_Updated() {
        Long userId = 1L;
        UpdateTermsRequestDTO request = new UpdateTermsRequestDTO(Terms.BODY_DATA_COLLECT, true);
        User user = User.builder().email("test@test.com").name("tester").build();
        UserTerms existingUserTerms = UserTerms.builder()
                .userTermsId(10L)
                .user(user)
                .terms(Terms.BODY_DATA_COLLECT)
                .agreed(false)
                .agreedAt(LocalDateTime.now().minusDays(1))
                .build();

        given(userTermsRepository.findByUser_UserIdAndTerms(userId, Terms.BODY_DATA_COLLECT))
                .willReturn(Optional.of(existingUserTerms));

        myPageService.updateTermsAgreement(userId, request);

        assertThat(existingUserTerms.getAgreed()).isTrue();
    }

    @Test
    @DisplayName("기존에 약관 동의 내역이 없는 경우 새로 엔티티가 생성되어 저장된다")
    void updateTermsAgreement_NewTerms_Saved() {
        Long userId = 1L;
        UpdateTermsRequestDTO request = new UpdateTermsRequestDTO(Terms.BODY_DATA_COLLECT, true);
        User user = User.builder().email("test@test.com").name("tester").build();

        given(userTermsRepository.findByUser_UserIdAndTerms(userId, Terms.BODY_DATA_COLLECT))
                .willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        myPageService.updateTermsAgreement(userId, request);

        verify(userTermsRepository).save(any(UserTerms.class));
    }
}
