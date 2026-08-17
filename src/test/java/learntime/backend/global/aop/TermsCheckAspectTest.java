package learntime.backend.global.aop;

import learntime.backend.domain.exercise.error.code.ExerciseErrorCode;
import learntime.backend.domain.exercise.error.exception.ExerciseException;
import learntime.backend.domain.user.enums.Terms;
import learntime.backend.domain.user.repository.UserTermsRepository;
import learntime.backend.global.annotation.RequireTerms;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.security.CustomUserDetails;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TermsCheckAspectTest {

    @Mock
    private UserTermsRepository userTermsRepository;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private TermsCheckAspect termsCheckAspect;

    private static class SampleTarget {
        @RequireTerms(Terms.BODY_DATA_COLLECT)
        public void exerciseMethod() {}
    }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        Method method = SampleTarget.class.getMethod("exerciseMethod");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보가 없는 경우 UNAUTHORIZED_ACCESS 예외가 발생한다")
    void checkTermsAgreement_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> termsCheckAspect.checkTermsAgreement(joinPoint))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(AuthErrorCode.UNAUTHORIZED_ACCESS.getMessage());
    }

    @Test
    @DisplayName("선택 약관에 미동의한 경우 TERMS_NOT_AGREED_EXERCISE 예외가 발생한다")
    void checkTermsAgreement_NotAgreed_ThrowsException() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@test.com", "tester", "pass", "ROLE_USER", false);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        SecurityContextHolder.setContext(context);

        given(userTermsRepository.existsByUser_UserIdAndTermsAndAgreedTrue(1L, Terms.BODY_DATA_COLLECT))
                .willReturn(false);

        assertThatThrownBy(() -> termsCheckAspect.checkTermsAgreement(joinPoint))
                .isInstanceOf(ExerciseException.class)
                .hasMessageContaining(ExerciseErrorCode.TERMS_NOT_AGREED_EXERCISE.getMessage());
    }

    @Test
    @DisplayName("선택 약관에 동의한 경우 예외 없이 정상 통과한다")
    void checkTermsAgreement_Agreed_Success() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@test.com", "tester", "pass", "ROLE_USER", false);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        SecurityContextHolder.setContext(context);

        given(userTermsRepository.existsByUser_UserIdAndTermsAndAgreedTrue(1L, Terms.BODY_DATA_COLLECT))
                .willReturn(true);

        assertThatCode(() -> termsCheckAspect.checkTermsAgreement(joinPoint))
                .doesNotThrowAnyException();
    }
}
