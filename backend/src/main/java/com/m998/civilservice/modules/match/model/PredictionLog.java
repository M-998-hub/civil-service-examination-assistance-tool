package com.m998.civilservice.modules.match.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 预测记录表
 * </p>
 *
 * @since 2026-04-10
 */
@Getter
@Setter
@TableName("prediction_log")
@ApiModel(value = "PredictionLog对象", description = "预测记录表")
public class PredictionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("岗位ID")
    private Long positionId;

    @ApiModelProperty("模拟考分数")
    private BigDecimal simulateScore;

    @ApiModelProperty("预测概率")
    private BigDecimal probability;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;


}
