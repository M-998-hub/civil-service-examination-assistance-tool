package com.macro.mall.tiny.modules.ums.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户档案参数
 * Created by macro on 2026-04-03.
 */
@Getter
@Setter
public class UserArchiveParam {
    @ApiModelProperty(value = "专业")
    private String major;

    @ApiModelProperty(value = "学历（本科/硕士/博士）")
    private String education;

    @ApiModelProperty(value = "政治面貌（党员/团员/群众）")
    private String politicalStatus;

    @ApiModelProperty(value = "是否应届（0否1是）")
    private Boolean isFreshGraduate;
}
