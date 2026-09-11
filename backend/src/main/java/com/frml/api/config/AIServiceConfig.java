package com.frml.api.config;

import com.frml.api.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AIServiceConfig {
    @Autowired
    private Environment env;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public AIService aiService() {
        String serviceType = env.getProperty("ai.service.type", "dify");
        switch (serviceType) {
            case "dify":
                return applicationContext.getBean("dify", AIService.class);
            default:
                throw new IllegalArgumentException("Unsupported service type: " + serviceType);
        }
    }
}
