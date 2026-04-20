package com.macro.mall.tiny.modules.ums.dto;

import lombok.Data;

import java.util.List;

/**
 * 上传预览结果
 * Created by macro on 2026-04-03.
 */
@Data
public class UploadResult {

    /**
     * 会话ID，用于后续导入
     */
    private String sessionId;

    /**
     * Excel列名列表
     */
    private List<String> columns;

    /**
     * 预览数据（前5行）
     */
    private List<List<String>> previewData;

    /**
     * 总行数
     */
    private int totalRows;
}
