package learntime.backend.global.config.aws.local;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("local")
@Configuration
public class LocalDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String userName;

    // application-local.yml의 SPRING_DATASOURCE_PASSWORD 환경변수로 주입
    // local 환경: 동일한 RDS endpoint에 일반 MySQL 비밀번호로 접속
    // AWS SDK 관련 코드 없음 → RdsClient, IAM 토큰 생성 불필요
    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    public HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        return dataSource;
    }
}
