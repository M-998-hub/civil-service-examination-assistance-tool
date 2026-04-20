package com.macro.mall.tiny.modules.ums.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.macro.mall.tiny.modules.ums.dto.ImportResult;
import com.macro.mall.tiny.modules.ums.dto.PositionFilterParam;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 岗位表 服务类
 * </p>
 *
 * @author macro
 * @since 2026-04-10
 */
public interface PositionService extends IService<Position> {

    /**
     * 根据条件筛选岗位
     */
    Page<Position> filter(PositionFilterParam param);

    /**
     * 导入 Excel 岗位数据
     */
    ImportResult importExcel(MultipartFile file) throws Exception;
}
