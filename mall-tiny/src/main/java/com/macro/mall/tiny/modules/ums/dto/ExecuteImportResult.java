package com.macro.mall.tiny.modules.ums.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行导入结果
 * Created by macro on 2026-04-03.
 */
@Data
public class ExecuteImportResult {

    /**
     * 成功条数
     */
    private int successCount;

    /**
     * 失败条数
     */
    private int failCount;

    /**
     * 跳过条数（重复数据）
     */
    private int skipCount;

    /**
     * 失败详情
     */
    private List<FailDetail> failDetails = new ArrayList<>();

    /**
     * 失败详情
     */
    @Data
    public static class FailDetail {
        /**
         * 行号
         */
        private int rowNum;

        /**
         * 数据内容
         */
        private String data;

        /**
         * 失败原因
         */
        private String reason;

        public FailDetail() {
        }

        public FailDetail(int rowNum, String data, String reason) {
            this.rowNum = rowNum;
            this.data = data;
            this.reason = reason;
        }
    }

    public void addFailDetail(int rowNum, String data, String reason) {
        this.failDetails.add(new FailDetail(rowNum, data, reason));
    }
}
