package com.macro.mall.tiny.modules.ums.dto;

import com.macro.mall.tiny.modules.ums.model.Position;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 岗位匹配结果DTO
 * Created by macro on 2026-04-03.
 */
@Getter
@Setter
public class MatchResultDto {
    @ApiModelProperty("岗位信息")
    private Position position;

    @ApiModelProperty("匹配度分数")
    private Integer matchScore;

    @ApiModelProperty("匹配详情")
    private List<String> matchDetails;

    public MatchResultDto(Position position, Integer matchScore, List<String> matchDetails) {
        this.position = position;
        this.matchScore = matchScore;
        this.matchDetails = matchDetails;
    }
}
