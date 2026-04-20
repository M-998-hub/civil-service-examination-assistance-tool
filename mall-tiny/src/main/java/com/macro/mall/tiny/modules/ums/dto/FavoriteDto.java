package com.macro.mall.tiny.modules.ums.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 收藏DTO，包含岗位信息和收藏时间
 * Created by macro on 2026-04-03.
 */
@Getter
@Setter
public class FavoriteDto {
    @ApiModelProperty("岗位ID")
    private Long positionId;

    @ApiModelProperty("岗位名称")
    private String positionName;

    @ApiModelProperty("部门")
    private String department;

    @ApiModelProperty("年份")
    private Integer year;

    @ApiModelProperty("收藏时间")
    private Date favoriteTime;

    @ApiModelProperty("学历要求")
    private String educationRequired;

    @ApiModelProperty("专业要求")
    private String majorRequired;

    @ApiModelProperty("招录人数")
    private Integer recruitmentNumber;
}
