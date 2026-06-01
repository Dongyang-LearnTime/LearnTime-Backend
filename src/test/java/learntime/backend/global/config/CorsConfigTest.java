package learntime.backend.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CorsConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("CORS Configuration should load the allowed origins correctly from the properties")
    void corsConfigurationShouldLoadAllowedOrigins() {
        // given
        UrlBasedCorsConfigurationSource source = securityConfig.corsConfigurationSource();

        // when
        CorsConfiguration config = source.getCorsConfigurations().get("/**");

        // then
        assertThat(config).isNotNull();
        List<String> allowedOrigins = config.getAllowedOrigins();
        assertThat(allowedOrigins)
                .isNotNull()
                .contains("http://localhost:5173");
    }
}
