package learntime.backend.global.config.aws.prod;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;

@Profile("prod")
@Configuration
public class ProdAwsRdsConfig {

    // DefaultCredentialsProvider: EC2 Instance Profile → 환경변수 → ~/.aws/credentials 순으로 자동 조회
    // @Profile("prod")로 인해 local 환경에서는 이 Bean 자체가 생성되지 않음
    // → local 기동 시 AWS SDK 네트워크 초기화 비용 및 자격증명 조회 타임아웃 없음
    @Bean
    public RdsClient rdsClient() {
        return RdsClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}
