package com.m998.civilservice.modules.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.m998.civilservice.config.AiConfig;
import com.m998.civilservice.modules.ai.dto.AiResponse;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import com.m998.civilservice.modules.ai.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final String SYSTEM_PROMPT = "你是考公选岗分析专家，根据用户档案和岗位信息，给出简洁专业的分析：1)匹配优势 2)潜在风险 3)报考建议。每次回答控制在200字以内。";

    @Autowired
    private AiConfig aiConfig;

    // Java工具类，RestTemplate负责发HTTP请求，ObjectMapper负责对JSON与Java对象进行转换
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 岗位匹配分析
    @Override
    public AiResponse analyzeMatch(String apiKey, UserArchive archive, Position position, Integer matchScore, List<String> matchDetails) {
        // 根据用户档案 + 岗位信息 + 系统匹配分数生成userPrompt
        String userPrompt = buildAnalyzePrompt(archive, position, matchScore, matchDetails);
        // 调用 AI API生成回答
        String answer = callDeepSeek(apiKey, userPrompt);
        // 返回 AI 生成的分析报告
        AiResponse resp = new AiResponse();
        resp.setAnswer(answer);
        return resp;
    }

    // 用户自由提问，AI根据用户档案提供个性化回答
    @Override
    public AiResponse ask(String apiKey, String question, UserArchive archive) {
        String userPrompt = buildAskPrompt(question, archive);
        String answer = callDeepSeek(apiKey, userPrompt);
        AiResponse resp = new AiResponse();
        resp.setAnswer(answer);
        return resp;
    }

    private String buildAnalyzePrompt(UserArchive archive, Position position, Integer matchScore, List<String> matchDetails) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户档案：\n");
        sb.append("- 专业：").append(archive.getMajor() != null ? archive.getMajor() : "未填写").append("\n");
        sb.append("- 学历：").append(archive.getEducation() != null ? archive.getEducation() : "未填写").append("\n");
        sb.append("- 政治面貌：").append(archive.getPoliticalStatus() != null ? archive.getPoliticalStatus() : "未填写").append("\n");
        sb.append("- 是否应届：").append(archive.getIsFreshGraduate() != null && archive.getIsFreshGraduate() ? "是" : "否").append("\n\n");

        sb.append("目标岗位：\n");
        sb.append("- 岗位名称：").append(position.getPositionName()).append("\n");
        sb.append("- 招录部门：").append(position.getDepartment()).append("\n");
        sb.append("- 专业要求：").append(position.getMajorRequired() != null ? position.getMajorRequired() : "不限").append("\n");
        sb.append("- 学历要求：").append(position.getEducationRequired() != null ? position.getEducationRequired() : "不限").append("\n");
        sb.append("- 政治面貌要求：").append(position.getPoliticalStatusRequired() != null ? position.getPoliticalStatusRequired() : "不限").append("\n");
        sb.append("- 招录人数：").append(position.getRecruitmentNumber()).append("\n\n");

        sb.append("系统匹配结果：匹配度 ").append(matchScore).append("分\n");
        sb.append("匹配详情：").append(String.join("，", matchDetails != null ? matchDetails : Collections.emptyList()));
        sb.append("\n\n请分析该岗位与用户档案的匹配情况。");

        return sb.toString();
    }

    private String buildAskPrompt(String question, UserArchive archive) {
        StringBuilder sb = new StringBuilder();
        if (archive != null) {
            sb.append("用户档案：专业");
            if (archive.getMajor() != null) sb.append(archive.getMajor());
            else sb.append("未填写");
            sb.append("，学历");
            if (archive.getEducation() != null) sb.append(archive.getEducation());
            else sb.append("未填写");
            sb.append("，政治面貌");
            if (archive.getPoliticalStatus() != null) sb.append(archive.getPoliticalStatus());
            else sb.append("未填写");
            sb.append("，");
            sb.append(archive.getIsFreshGraduate() != null && archive.getIsFreshGraduate() ? "应届" : "非应届");
            sb.append("。\n\n");
        }
        sb.append("用户提问：").append(question);
        return sb.toString();
    }

    // 调用 AI API
    private String callDeepSeek(String apiKey, String userPrompt) {
        try {
            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiConfig.getModel()); // 模型名称

            // 构建消息列表(System + User)
            List<Map<String, String>> messages = new ArrayList<>();

            // System 消息：定义 AI 和角色的行为
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", SYSTEM_PROMPT);
            messages.add(sysMsg);

            // User 消息：用户的具体问题
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            // 设置参数
            body.put("messages", messages);
            body.put("temperature", 0.7); // 控制随机性（0-1，越高越多样）
            body.put("max_tokens", 600); // 最大输出长度

            // 设置请求体
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey); // API 认证

            // 构建请求实体（身体 + 头）
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            // 记录日志（跟踪请求）
            LOGGER.info("Calling DeepSeek API, promptLength={}", userPrompt.length());

            // 发送请求并接收响应
            ResponseEntity<String> response = restTemplate.exchange(aiConfig.getApiUrl(), HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 解析响应
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null) {
                        String content = message.get("content").asText();
                        return content != null ? content.trim() : "AI 未返回有效回复";
                    }
                }
            }
            return "AI 服务返回异常，请稍后重试";
        } catch (Exception e) {
            LOGGER.error("DeepSeek API call failed", e);
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return "API Key 无效，请检查后重试";
            }
            if (e.getMessage() != null && (e.getMessage().contains("timeout") || e.getMessage().contains("connect"))) {
                return "AI 服务连接超时，请稍后重试";
            }
            return "AI 服务暂时不可用：" + (e.getMessage() != null ? e.getMessage() : "未知错误");
        }
    }
}
