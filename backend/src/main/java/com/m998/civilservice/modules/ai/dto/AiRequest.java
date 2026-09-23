package com.m998.civilservice.modules.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiRequest {
    private String apiKey;
    private String question;
    private Long positionId;
    private Integer matchScore;
    private List<String> matchDetails;
}
