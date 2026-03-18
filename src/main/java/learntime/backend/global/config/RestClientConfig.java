package learntime.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)) // 연결 자체에 걸리는 최대 시간(3초)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(60)); // 데이터 수신 대기 최대 시간(60초)

        // 기본 베이스 URL 및 공통 헤더 등 기본 설정
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/json, application/xml;q=0.9")
                .defaultHeader("User-Agent", "learn-time-server")
                .build();
    }
}
