package learntime.backend.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;


@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {

        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(List.of(
                new CaffeineCache(
                        "studyTotalIndicator",
                        Caffeine.newBuilder()
                                .expireAfterWrite(10, TimeUnit.MINUTES)
                                .maximumSize(1000)
                                .build()
                ),
                new CaffeineCache(
                        "studyRecentWeekInfo",
                        Caffeine.newBuilder()
                                .expireAfterWrite(1, TimeUnit.HOURS)
                                .maximumSize(1000)
                                .build()
                )
        ));

        return cacheManager;
    }
}
