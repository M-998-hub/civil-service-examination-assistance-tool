package com.macro.mall.tiny.modules.ums.service;

import com.macro.mall.tiny.modules.ums.dto.ExecuteImportParam;
import com.macro.mall.tiny.modules.ums.dto.ExecuteImportResult;
import com.macro.mall.tiny.modules.ums.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端数据导入服务
 * Created by macro on 2026-04-03.
 */
public interface AdminImportService {

    /**
     * 上传文件并预览
     * @param file Excel文件
     * @return 预览结果
     */
    UploadResult uploadAndPreview(MultipartFile file) throws Exception;

    /**
     * 执行导入
     * @param param 导入参数
     * @param userId 用户ID
     * @return 导入结果
     */
    ExecuteImportResult executeImport(ExecuteImportParam param, Long userId) throws Exception;

    /**
     * 清理过期的临时文件
     */
    void cleanExpiredFiles();
}
