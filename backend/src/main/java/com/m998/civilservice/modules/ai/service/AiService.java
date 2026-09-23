package com.m998.civilservice.modules.ai.service;

import com.m998.civilservice.modules.ai.dto.AiResponse;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;

import java.util.List;

public interface AiService {
    AiResponse analyzeMatch(String apiKey, UserArchive archive, Position position, Integer matchScore, List<String> matchDetails);
    AiResponse ask(String apiKey, String question, UserArchive archive);
}
