package com.example.SmartHospital.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory); // spring.redis.host + spring.redis.port
        // EX: "auth:token:12345"
        template.setKeySerializer(RedisSerializer.string()); 
        // EX: {"id":"1","name":"Ngoc","roles":["ADMIN"]}
        template.setValueSerializer(RedisSerializer.json()); 
        // HSET user:1 email "test@gmail.com"
        template.setHashKeySerializer(RedisSerializer.string());
        // HSET user:1 profile {"age":20,"gender":"female"}
        template.setHashValueSerializer(RedisSerializer.json());
        return template;
    }
}
