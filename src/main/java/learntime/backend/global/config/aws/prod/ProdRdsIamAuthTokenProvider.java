package learntime.backend.global.config.aws.prod;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class ProdRdsIamAuthTokenProvider {

    private final RdsClient rdsClient;

    @Value("${aws.rds.host-name}")
    private String hostname;

    @Value("${spring.datasource.port}")
    private int port;

    @Value("${spring.datasource.username}")
    private String userName;

    // AWS STS 서명 기반 임시 토큰 생성
    // 유효시간: 15분 (AWS 고정값, 변경 불가)
    // 이 토큰이 MySQL 접속 시 password 역할을 함
    public String generateToken() {
        RdsUtilities utilities = rdsClient.utilities();
        return utilities.generateAuthenticationToken(
                GenerateAuthenticationTokenRequest.builder()
                        .hostname(hostname)
                        .port(port)
                        .username(userName)
                        .build()
        );
    }
}
