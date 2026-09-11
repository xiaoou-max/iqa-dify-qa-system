package com.frml.api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 提前初始化 Lettuce（推荐）
 * 让 Redis 客户端在应用启动时而非首次请求时完成初始化，避免阻塞首次请求。
 * 可以通过配置一个启动时执行的任务，主动触发 Redis 连接.
 * 否则服务启动后第一次调用接口很可能额会出现超时报错的情况
 */
@Component
public class RedisPreloader implements CommandLineRunner {

    // 改为注入你自定义的 RedisTemplate<String, Object> 类型
    private final RedisTemplate<String, Object> adiRedisTemplate;

    // 构造函数注入自定义的 RedisTemplate
    public RedisPreloader(RedisTemplate<String, Object> adiRedisTemplate) {
        this.adiRedisTemplate = adiRedisTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // 启动时通过自定义 RedisTemplate 触发 Redis 连接初始化
        adiRedisTemplate.getConnectionFactory().getConnection().ping();
    }
}
