package com.m998.civilservice.modules.importdata.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.m998.civilservice.modules.importdata.model.ImportTemplate;

import java.util.List;

/**
 * 导入模板服务
 */
public interface ImportTemplateService extends IService<ImportTemplate> {

    /**
     * 获取所有模板
     */
    List<ImportTemplate> listAll();

    /**
     * 根据导入类型获取模板列表
     */
    List<ImportTemplate> listByImportType(String importType);

    /**
     * 根据名称获取模板
     */
    ImportTemplate getByName(String templateName);

    /**
     * 保存模板
     */
    boolean saveTemplate(ImportTemplate template, Long userId);
}
