package com.macro.mall.tiny.modules.ums.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入字段元数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportFieldMeta {

    /**
     * 字段名（如 department, positionName）
     */
    private String fieldName;

    /**
     * 字段中文标签
     */
    private String label;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 字段描述说明
     */
    private String description;
}
