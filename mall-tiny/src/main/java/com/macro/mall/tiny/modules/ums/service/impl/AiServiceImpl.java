package com.macro.mall.tiny.modules.ums.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.tiny.config.AiConfig;
import com.macro.mall.tiny.modules.ums.dto.AiResponse;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.model.UserArchive;
import com.macro.mall.tiny.modules.ums.service.AiService;
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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiResponse analyzeMatch(String apiKey, UserArchive archive, Position position, Integer matchScore, List<String> matchDetails) {
        String userPrompt = buildAnalyzePrompt(archive, position, matchScore, matchDetails);
        String answer = callDeepSeek(apiKey, userPrompt);
        AiResponse resp = new AiResponse();
        resp.setAnswer(answer);
        return resp;
    }

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

    private String callDeepSeek(String apiKey, String userPrompt) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiConfig.getModel());

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", SYSTEM_PROMPT);
            messages.add(sysMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 600);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            LOGGER.info("Calling DeepSeek API for prompt: {}", userPrompt.substring(0, Math.min(100, userPrompt.length())));

            ResponseEntity<String> response = restTemplate.exchange(aiConfig.getApiUrl(), HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
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
