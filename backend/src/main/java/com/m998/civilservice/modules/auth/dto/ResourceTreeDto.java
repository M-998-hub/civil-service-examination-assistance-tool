package com.m998.civilservice.modules.auth.dto;

import com.m998.civilservice.modules.auth.model.UmsResource;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 资源树DTO（按分类分组）
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
