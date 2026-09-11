package com.frml.api.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @Author: 用智能拥抱幸福
 * @Description: druid数据库连接池配置
 * @Date: 5/21/021 22:32
 */
@Configuration
public class DruidConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.druid") // 前缀需与配置文件一致
    public DataSource druidDataSource() {
        return new DruidDataSource();
    }
}
