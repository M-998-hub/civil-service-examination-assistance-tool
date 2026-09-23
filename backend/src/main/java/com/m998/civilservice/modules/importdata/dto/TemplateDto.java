package com.m998.civilservice.modules.importdata.dto;

import lombok.Data;

/**
 * 模板DTO
 */
@Data
public class TemplateDto {

    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 列映射JSON
     */
    private String columnMapping;

    /**
     * 导入类型
     */
    private String importType;
}
