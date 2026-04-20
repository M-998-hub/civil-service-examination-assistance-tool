package com.macro.mall.tiny.modules.ums.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用户档案表
 * </p>
 *
 * @author macro
 * @since 2026-04-10
 */
@Getter
@Setter
@TableName("user_archive")
@ApiModel(value = "UserArchive对象", description = "用户档案表")
public class UserArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("关联 ums_admin 表")
    private Long userId;

    @ApiModelProperty("专业")
    private String major;

    @ApiModelProperty("学历（本科/硕士/博士）")
    private String education;

    @ApiModelProperty("政治面貌（党员/团员/群众）")
    private String politicalStatus;

    @ApiModelProperty("是否应届（0否1是）")
    private Boolean isFreshGraduate;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;


}
