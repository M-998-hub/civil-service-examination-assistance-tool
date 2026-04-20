package com.macro.mall.tiny.modules.ums.dto;

import lombok.Data;

import java.util.Map;

/**
 * 执行导入参数
 * Created by macro on 2026-04-03.
 */
@Data
public class ExecuteImportParam {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 列映射关系
     * key: 字段名（department, positionName, majorRequired, educationRequired, 
     *          politicalStatusRequired, isFreshOnly, recruitmentNumber）
     * value: Excel列索引（从0开始）
     * 注意：year 字段不再使用映射，改用 year 字段直接传入
     */
    private Map<String, Object> mapping;

    /**
     * 年份（手动输入）
     */
    private String year;

    /**
     * 模板名称（可选，用于保存模板）
     */
    private String templateName;

    /**
     * 是否保存为模板
     */
    private Boolean saveAsTemplate;
}
