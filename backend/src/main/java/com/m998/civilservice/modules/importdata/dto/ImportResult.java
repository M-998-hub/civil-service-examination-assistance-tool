package com.m998.civilservice.modules.importdata.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果
 */
@Data
public class ImportResult {

    /**
     * 成功条数
     */
    private int successCount;

    /**
     * 失败条数
     */
    private int failCount;

    /**
     * 失败原因列表
     */
    private List<String> failReasons = new ArrayList<>();

    public ImportResult() {
    }

    public ImportResult(int successCount, int failCount) {
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public void addFailReason(String reason) {
        this.failReasons.add(reason);
    }
}
