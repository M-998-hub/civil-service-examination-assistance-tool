package com.macro.mall.tiny.modules.ums.dto;

import com.macro.mall.tiny.modules.ums.model.UmsResource;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 资源树DTO（按分类分组）
 * Created by macro on 2026-04-03.
 */
@Data
public class ResourceTreeDto {
    @ApiModelProperty(value = "分类ID")
    private Long categoryId;

    @ApiModelProperty(value = "分类名称")
    private String categoryName;

    @ApiModelProperty(value = "分类下的资源列表")
    private List<UmsResource> resources;
}
