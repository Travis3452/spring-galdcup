package com.example.galdcup.common.config;

import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis 기반 인프라 구축 및 데이터 접근 설정
 */
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    /** Lettuce 라이브러리를 이용한 Redis 연결 팩토리 생성 */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);
        redisConfig.setPassword(password);
        return new LettuceConnectionFactory(redisConfig);
    }

    /** Bucket4j-redis 확장에서 요구하는 Redis 연결 클라이언트 생성 */
    @Bean
    public RedisClient redisClient() { return RedisClient.create(String.format("redis://%s:%d", host, port)); }

    /** 애플리케이션 공통 Redis 데이터 조작 템플릿 정의 */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        /**
         * * JSON 직렬화 설정
         * 날짜 형식(ISO 8601) 유지 및 역직렬화 시 타입 정보(@class) 포함 전략 적용
         */
        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.create(serializerBuilder -> {
            serializerBuilder.customize(mapperBuilder -> {
                mapperBuilder
                        .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                        .activateDefaultTypingAsProperty(
                                BasicPolymorphicTypeValidator.builder()
                                        .allowIfBaseType(Object.class)
                                        .build(),
                                DefaultTyping.NON_FINAL,
                                "@class"
                        );
            });
        });

        // Key와 HashKey는 문자열 직렬화 방식 사용
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value는 객체 저장을 위해 커스텀 JSON 직렬화 방식 사용
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(new StringRedisSerializer());

        return template;
    }
}