package learntime.backend.global.config.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;

@Configuration
public class AwsRdsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public RdsClient rdsClient() {

        // AWS SDK Client는 Thread-safe
        // Singleton Bean으로 재사용하는 것이 공식 권장 방식
        return RdsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}