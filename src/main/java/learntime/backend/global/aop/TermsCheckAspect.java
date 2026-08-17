package learntime.backend.global.aop;

import learntime.backend.domain.exercise.error.code.ExerciseErrorCode;
import learntime.backend.domain.exercise.error.exception.ExerciseException;
import learntime.backend.domain.user.enums.Terms;
import learntime.backend.domain.user.repository.UserTermsRepository;
import learntime.backend.global.annotation.RequireTerms;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TermsCheckAspect {

    private final UserTermsRepository userTermsRepository;

    @Before("@within(learntime.backend.global.annotation.RequireTerms) || @annotation(learntime.backend.global.annotation.RequireTerms)")
    public void checkTermsAgreement(JoinPoint joinPoint) {
        // 메서드 또는 클래스에 선언된 @RequireTerms 어노테이션 추출
        RequireTerms requireTerms = extractRequireTerms(joinPoint);
        if (requireTerms == null) {
            return;
        }

        Terms targetTerms = requireTerms.value();

        // Spring Security 인증 객체에서 로그인된 사용자 정보(CustomUserDetails) 및 userId 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED_ACCESS);
        }

        Long userId = userDetails.userId();

        // DB 단일 exists 쿼리를 통해 해당 사용자가 대상 약관에 동의(agreed == true)했는지 확인
        boolean isAgreed = userTermsRepository.existsByUser_UserIdAndTermsAndAgreedTrue(userId, targetTerms);
        if (!isAgreed) {

            // 미동의 시 도메인 예외를 던져 접근 차단 (403 Forbidden 응답)
            if (targetTerms == Terms.BODY_DATA_COLLECT) {
                throw new ExerciseException(ExerciseErrorCode.TERMS_NOT_AGREED_EXERCISE);
            }
            throw new AuthException(AuthErrorCode.TERMS_NOT_AGREED);
        }
    }

    /**
     * JoinPoint의 메서드 레벨 어노테이션을 우선 탐색하고, 없을 경우 클래스 레벨 어노테이션을 탐색합니다.
     */
    private RequireTerms extractRequireTerms(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 메서드 레벨 어노테이션 우선 확인 (세부 설정 우선)
        RequireTerms methodAnnotation = AnnotationUtils.findAnnotation(method, RequireTerms.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        // 2. 클래스 레벨 어노테이션 확인 (컨트롤러 일괄 적용)
        return AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RequireTerms.class);
    }
}
