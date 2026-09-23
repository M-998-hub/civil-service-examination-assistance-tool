package com.m998.civilservice.modules.match.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Component
public class MatchConfig {

    private Map<String, Integer> educationLevels = Collections.emptyMap();
    private Map<String, Integer> politicalStatusLevels = Collections.emptyMap();
    private Map<String, List<String>> majorCategories = Collections.emptyMap();
    private Map<String, Integer> scoreWeights = Collections.emptyMap();

    @PostConstruct  //保证该方法在Bean初始化完成后自动执行
    public void init() {
        try {
            // 创建 Jackson 的ObjectMapper(Json解释器)
            ObjectMapper mapper = new ObjectMapper();
            // 从 classpath 读取 match-config.json 文件
            InputStream is = new ClassPathResource("match-config.json").getInputStream();
            // 将整个 JSON 解析为 Map<String,Object>
            Map<String, Object> raw = mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
            // 分别提取各字段并转换为对应的泛型类型并赋值给成员变量
            this.educationLevels = mapper.convertValue(raw.get("educationLevels"), new TypeReference<Map<String, Integer>>() {});
            this.politicalStatusLevels = mapper.convertValue(raw.get("politicalStatusLevels"), new TypeReference<Map<String, Integer>>() {});
            this.majorCategories = mapper.convertValue(raw.get("majorCategories"), new TypeReference<Map<String, List<String>>>() {});
            this.scoreWeights = mapper.convertValue(raw.get("scoreWeights"), new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            // 失败则抛出运行时异常
            throw new RuntimeException("Failed to load match-config.json", e);
        }
    }
}
