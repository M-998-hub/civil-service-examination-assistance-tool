package com.macro.mall.tiny.modules.ums.strategy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 导入类型枚举
 */
public enum ImportTypeEnum {

    POSITION("position", "岗位信息"),
    POSITION_STATS("position_stats", "报录比数据");

    private final String code;
    private final String label;

    ImportTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ImportTypeEnum fromCode(String code) {
        for (ImportTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的导入类型: " + code);
    }
}
