package com.m998.civilservice.modules.position.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 岗位筛选参数
 */
@Getter
@Setter
public class PositionFilterParam {
    @ApiModelProperty(value = "年份")
    private Integer year;

    @ApiModelProperty(value = "部门名称（模糊匹配）")
    private String department;

    @ApiModelProperty(value = "学历要求（精确匹配）")
    private String educationRequired;

    @ApiModelProperty(value = "学历等级筛选（大专=1，本科=2，硕士=3，博士=4），查询该等级及以下可报岗位")
    private Integer educationLevel;

    @ApiModelProperty(value = "政治面貌要求（精确匹配）")
    private String politicalStatusRequired;

    @ApiModelProperty(value = "政治面貌等级筛选（不限=1，共青团员=2，中共党员=3），查询该等级及以下可报岗位")
    private Integer politicalStatusLevel;

    @ApiModelProperty(value = "是否限应届（0/1）")
    private Boolean isFreshOnly;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页数量", example = "10")
    private Integer pageSize = 10;
}
