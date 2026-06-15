package com.macro.mall.tiny.modules.ums.dto;

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
