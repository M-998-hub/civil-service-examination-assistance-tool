package com.macro.mall.tiny.modules.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.macro.mall.tiny.modules.ums.model.ImportTemplate;

import java.util.List;

/**
 * 导入模板服务
 * Created by macro on 2026-04-03.
 */
public interface ImportTemplateService extends IService<ImportTemplate> {

    /**
     * 获取所有模板
     */
    List<ImportTemplate> listAll();

    /**
     * 根据名称获取模板
     */
    ImportTemplate getByName(String templateName);

    /**
     * 保存模板
     */
    boolean saveTemplate(ImportTemplate template, Long userId);
}
