package com.macro.mall.tiny.modules.ums.service;

import com.macro.mall.tiny.modules.ums.dto.AiResponse;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.model.UserArchive;

import java.util.List;

public interface AiService {
    AiResponse analyzeMatch(String apiKey, UserArchive archive, Position position, Integer matchScore, List<String> matchDetails);
    AiResponse ask(String apiKey, String question, UserArchive archive);
}
