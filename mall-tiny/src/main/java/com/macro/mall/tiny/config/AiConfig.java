package com.macro.mall.tiny.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class AiConfig {
    private String apiUrl = "https://api.deepseek.com/chat/completions";
    private String model = "deepseek-chat";
}
