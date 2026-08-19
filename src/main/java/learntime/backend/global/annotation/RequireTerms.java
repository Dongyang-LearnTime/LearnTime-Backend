package learntime.backend.global.annotation;

import learntime.backend.domain.user.enums.Terms;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 특정 약관의 동의 여부를 사전에 검증하는 AOP 어노테이션입니다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireTerms {
    Terms value();
}
