package org.yandex.mymarketapp.configuration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@EnableCaching
@Configuration
public class RedisConfiguration {


    @Bean
    public RedisCacheManagerBuilderCustomizer weatherCacheCustomizer() {
        return builder -> builder
                .withCacheConfiguration(
                        "item",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(30, ChronoUnit.MINUTES))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(Item.class)))
                ).withCacheConfiguration(
                        "item_page",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(30, ChronoUnit.MINUTES))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(ViewPage.class)))
                ).withCacheConfiguration(
                            "page_info",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(30, ChronoUnit.MINUTES))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(Paging.class)))
                ).withCacheConfiguration(
                        "cart_items",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(1, ChronoUnit.MINUTES))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(CartItemsDto.class)))
                ).withCacheConfiguration(
                        "orders",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(3, ChronoUnit.MINUTES))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(OrdersDto.class)))
                );
    }


}
