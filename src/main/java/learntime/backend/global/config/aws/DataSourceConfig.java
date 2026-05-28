package learntime.backend.global.config.aws;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final RdsIamAuthTokenProvider tokenProvider;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String userName;


    @Bean
    public DataSource dataSource() {

        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(userName);
        dataSource.setPassword(tokenProvider.generateToken());

        return dataSource;
    }
}