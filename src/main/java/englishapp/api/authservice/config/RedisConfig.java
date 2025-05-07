package englishapp.api.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        // Create serializers for key and value
        StringRedisSerializer serializer = new StringRedisSerializer();

        // Create RedisTemplate using serializers
        return new ReactiveRedisTemplate<>(
                connectionFactory,
                RedisSerializationContext
                        .<String, String>newSerializationContext()
                        .key(serializer)
                        .value(serializer)
                        .hashKey(serializer)
                        .hashValue(serializer)
                        .build());
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("redis-14025.c294.ap-northeast-1-2.ec2.redns.redis-cloud.com");
        config.setPort(14025);
        config.setPassword(RedisPassword.of("cCgwAqRWzyJEu9nptVhQsZcjSDSU6uMx"));
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }
}