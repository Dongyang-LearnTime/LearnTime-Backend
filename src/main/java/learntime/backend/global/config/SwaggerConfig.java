package learntime.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI swagger() {
        // 문서 기본 정보 설정
        Info info = new Info()
                .title("Learn-Time Project API")
                .description("Learn-Time의 REST API 명세서임.")
                .version("1.0.0");

        // 보안 스킴 식별자
        final String SECURITY_SCHEME_NAME = "bearerAuth";

        return new OpenAPI()
                .info(info)
                .servers(List.of(
                        new Server().url("/") // 현재 서버
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP) // HTTP 프로토콜 방식
                                .scheme("bearer")
                                .bearerFormat("JWT"))); // 문서에 JWT임을 명시
    }
}
