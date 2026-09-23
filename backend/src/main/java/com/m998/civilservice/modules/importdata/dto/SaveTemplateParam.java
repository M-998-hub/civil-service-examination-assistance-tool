package com.m998.civilservice.modules.importdata.dto;

import lombok.Data;

/**
 * 保存模板参数
 */
@Data
public class SaveTemplateParam {

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
     * 导入类型（position / position_stats）
     */
    private String importType;
}
