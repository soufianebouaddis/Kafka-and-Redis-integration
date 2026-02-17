package os.org.inventory_service.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${app.redis.stock-ttl}")
    private long stockTtl;
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(host);
        serverConfig.setPort(port);


        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ofMillis(100))
                .build();

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(serverConfig, clientConfig);
        factory.setShareNativeConnection(true);   // one shared native connection (Lettuce is thread-safe)
        factory.setValidateConnection(false);      // skip ping on each borrow (performance)
        return factory;
    }

    @Bean
    public tools.jackson.databind.ObjectMapper redisObjectMapper() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("os.org.inventory_service.**")
                .build();
        return JsonMapper.builder()
                .addModule(new tools.jackson.datatype.jsr310.JavaTimeModule())
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTypingAsProperty(
                        ptv,
                        DefaultTyping.NON_FINAL,
                        "@class"
                )
                .build();
    }

    @Bean
    public RedisSerializer<Object> redisValueSerializer(tools.jackson.databind.ObjectMapper redisObjectMapper) {
        return new GenericJacksonJsonRedisSerializer(redisObjectMapper);
    }



    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf, RedisSerializer<Object> redisValueSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);


        StringRedisSerializer strSer = new StringRedisSerializer();

        template.setKeySerializer(strSer);
        template.setHashKeySerializer(strSer);
        template.setValueSerializer(redisValueSerializer);
        template.setHashValueSerializer(redisValueSerializer);
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();

        return template;
    }



    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf,RedisSerializer<Object> redisValueSerializer) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(stockTtl))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }


}
