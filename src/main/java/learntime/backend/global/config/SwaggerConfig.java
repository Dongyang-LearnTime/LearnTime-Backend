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

        // 서버 환경 설정
        Server localServer = new Server().url("http://localhost:8080").description("Local Server");
        Server devServer = new Server().url("https://api.dev.umc.com").description("Dev Server");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, devServer)) // 리스트 형태로 여러 서버 등록 가능
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP) // HTTP 프로토콜 방식
                                .scheme("bearer")
                                .bearerFormat("JWT"))); // 문서에 JWT임을 명시
    }
}
