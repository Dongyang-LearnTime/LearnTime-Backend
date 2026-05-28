package learntime.backend.global.config.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

@Component
@RequiredArgsConstructor
public class RdsIamAuthTokenProvider {

    private final RdsClient rdsClient;

    @Value("${aws.rds.host-name}")
    private String hostname;

    @Value("${spring.datasource.port}")
    private int port;

    @Value("${spring.datasource.username}")
    private String userName;

    // db 비밀번호 대신 토큰으로 db 접속
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