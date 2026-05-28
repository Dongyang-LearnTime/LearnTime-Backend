package learntime.backend.global.config.aws.prod;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class ProdRdsIamAuthTokenRefresher {

    // HikariDataSource 타입으로 직접 주입 (DataSource 인터페이스에는 setPassword() 없음)
    private final HikariDataSource hikariDataSource;
    private final ProdRdsIamAuthTokenProvider tokenProvider;

    // IAM 토큰 유효기간: 15분 (AWS 고정값)
    // HikariCP max-lifetime: 600,000ms = 10분 (application-prod.yml)
    // → 10분마다 새 토큰을 DataSource에 주입
    // → HikariCP가 max-lifetime 도달 후 커넥션을 재생성할 때 항상 유효한 토큰 사용 보장
    @Scheduled(fixedRate = 600_000)
    public void refreshIamToken() {
        try {
            log.info("[IAM] RDS IAM Auth Token 갱신 시작");
            String newToken = tokenProvider.generateToken();
            hikariDataSource.setPassword(newToken);
            log.info("[IAM] RDS IAM Auth Token 갱신 완료");
        } catch (Exception e) {
            log.error("[IAM] RDS IAM Auth Token 갱신 실패", e);
        }
    }
}
