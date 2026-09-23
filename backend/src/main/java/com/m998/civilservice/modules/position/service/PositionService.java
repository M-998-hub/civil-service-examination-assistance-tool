package com.m998.civilservice.modules.position.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.m998.civilservice.modules.importdata.dto.ImportResult;
import com.m998.civilservice.modules.position.dto.PositionFilterParam;
import com.m998.civilservice.modules.position.model.Position;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 岗位表 服务类
 * </p>
 *
 * @since 2026-04-10
 */
public interface PositionService extends IService<Position> {

    /**
     * 根据条件筛选岗位（自动过滤已删除）
     */
    Page<Position> filter(PositionFilterParam param);

    /**
     * 导入 Excel 岗位数据
     */
    ImportResult importExcel(MultipartFile file) throws Exception;

    /**
     * 管理端分页查询（过滤 status=0）
     */
    Page<Position> adminPage(Integer pageNum, Integer pageSize, String department, Integer year);

    /**
     * 逻辑删除
     */
    boolean deleteById(Long id);

    /**
     * 批量逻辑删除
     */
    boolean batchDelete(List<Long> ids);
}
