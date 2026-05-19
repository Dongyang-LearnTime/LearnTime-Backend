package learntime.backend.global.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Slf4j
@Aspect
@Component
public class ControllerLoggingAspect {

    // Controller 계층 요청/응답 로깅
    @Around("execution(* learntime.backend..controller..*(..))")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {

        // 현재 스레드의 서블릿 요청 속성 가져오기
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest(); // 요청 객체 추출 (null 방지)

        String uri = request.getRequestURI(); // 요청 URI
        String httpMethod = request.getMethod(); // HTTP Method
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName(); // 실행 클래스명
        String methodName = joinPoint.getSignature().getName(); // 실행 메서드명
        long startTime = System.currentTimeMillis();

        log.info("API START | Method: [{}] | URI: [{}] | Controller: [{}.{}]",
                httpMethod, uri, className, methodName);

        try {
            // 실제 타겟 메서드(Controller의 요청 처리 메서드)를 호출하고 실행 결과를 반환받음
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            log.info("API END | [{}] [{}] | Target: [{}.{}] | Duration: [{}ms]",
                    httpMethod, uri, className, methodName, duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("API ERROR | [{}] [{}] | Target: [{}.{}] | Duration: [{}ms] | Error: [{}]",
                    httpMethod, uri, className, methodName, duration, e.getMessage());
            throw e;
        }
    }

}
