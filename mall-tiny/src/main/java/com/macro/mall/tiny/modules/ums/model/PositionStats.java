package com.macro.mall.tiny.modules.ums.model;

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
 * 报录比/分数线表
 * </p>
 *
 * @author macro
 * @since 2026-04-10
 */
@Getter
@Setter
@TableName("position_stats")
@ApiModel(value = "PositionStats对象", description = "报录比/分数线表")
public class PositionStats implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("年份")
    private Integer year;

    @ApiModelProperty("招录部门")
    private String department;

    @ApiModelProperty("职位名称")
    private String positionName;

    @ApiModelProperty("报名人数")
    private Integer registrationCount;

    @ApiModelProperty("最低进面分")
    private BigDecimal minEntryScore;

    @ApiModelProperty("最高进面分")
    private BigDecimal maxEntryScore;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;


}
