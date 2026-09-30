package com.m998.civilservice.modules.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NationalHeaderResolver {
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public NationalHeaderResolver(ObjectMapper mapper,
            @Value("${ingestion.ai.api-key:}") String apiKey,
            @Value("${deepseek.api-url}") String apiUrl,
            @Value("${deepseek.model}") String model) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    public Map<String, Integer> resolveDeterministically(Map<Integer, String> row) {
        Map<String, Integer> result = new HashMap<>();
        row.forEach((index, label) -> {
            if (label == null) return;
            String normalized = label.replaceAll("[\\s\\n\\r（）()：:]", "");
            if (containsAny(normalized, "职位代码", "岗位代码")) result.put("code", index);
            else if (containsAny(normalized, "招录机关", "招考部门", "部门名称", "用人司局")) result.put("department", index);
            else if (containsAny(normalized, "职位名称", "岗位名称", "招考职位")) result.put("name", index);
            else if (containsAny(normalized, "专业要求", "专业")) result.put("major", index);
            else if (containsAny(normalized, "学历要求", "学历")) result.put("education", index);
            else if (containsAny(normalized, "政治面貌")) result.put("political", index);
            else if (containsAny(normalized, "招考人数", "计划人数", "招录人数")) result.put("count", index);
            else if (containsAny(normalized, "招录考生类别", "考生类别", "招考对象", "招录对象", "应届")) result.put("fresh", index);
        });
        return result;
    }

    private boolean containsAny(String label, String... options) {
        return Arrays.stream(options).anyMatch(label::contains);
    }

    public Map<String, Integer> resolveWithAi(Map<Integer, String> header) {
        if (apiKey.trim().isEmpty()) throw new IllegalArgumentException("表头无法自动识别，且未配置服务端 INGESTION_AI_API_KEY");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", 0);
            body.put("response_format", new HashMap<String, String>() {{ put("type", "json_object"); }});
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "只根据以下表头索引映射字段，返回纯JSON对象，键只能是 code,department,name,major,education,political,count,fresh，值只能是已有的整数索引。不得推断岗位内容。表头=" + mapper.writeValueAsString(header));
            body.put("messages", Arrays.asList(message));
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(8000);
            factory.setReadTimeout(20000);
            String response = new RestTemplate(factory).postForObject(apiUrl, new HttpEntity<>(body, headers), String.class);
            JsonNode content = mapper.readTree(response).path("choices").path(0).path("message").path("content");
            JsonNode mapping = mapper.readTree(content.asText());
            Map<String, Integer> result = new LinkedHashMap<>();
            List<String> allowed = Arrays.asList("code", "department", "name", "major", "education", "political", "count", "fresh");
            for (String key : allowed) {
                JsonNode value = mapping.path(key);
                if (value.isInt() && header.containsKey(value.asInt())) result.put(key, value.asInt());
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 表头识别失败，需人工检查来源文件", e);
        }
    }
}
