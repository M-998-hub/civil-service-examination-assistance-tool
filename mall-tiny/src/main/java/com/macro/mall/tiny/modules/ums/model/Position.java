package com.macro.mall.tiny.modules.ums.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 岗位表
 * </p>
 *
 * @author macro
 * @since 2026-04-10
 */
@Getter
@Setter
@ApiModel(value = "Position对象", description = "岗位表")
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("招录部门")
    private String department;

    @ApiModelProperty("职位名称")
    private String positionName;

    @ApiModelProperty("专业要求")
    private String majorRequired;

    @ApiModelProperty("学历要求")
    private String educationRequired;

    @ApiModelProperty("政治面貌要求")
    private String politicalStatusRequired;

    @ApiModelProperty("是否限应届（0否1是）")
    private Boolean isFreshOnly;

    @ApiModelProperty("招录人数")
    private Integer recruitmentNumber;

    @ApiModelProperty("报名截止时间")
    private Date registrationDeadline;

    @ApiModelProperty("年份")
    private Integer year;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;


}
