package com.m998.civilservice.modules.importdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.m998.civilservice.modules.importdata.mapper.ImportTemplateMapper;
import com.m998.civilservice.modules.importdata.model.ImportTemplate;
import com.m998.civilservice.modules.importdata.service.ImportTemplateService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 导入模板服务实现
 */
@Service
public class ImportTemplateServiceImpl extends ServiceImpl<ImportTemplateMapper, ImportTemplate> implements ImportTemplateService {

    /**
     * 获取所有模板列表（按创建时间倒序）
     * @return
     */
    @Override
    public List<ImportTemplate> listAll() {
        return list(new LambdaQueryWrapper<ImportTemplate>()
                .orderByDesc(ImportTemplate::getCreateTime));
    }

    /**
     * 根据导入类型获取模板列表
     * @param importType
     * @return
     */
    @Override
    public List<ImportTemplate> listByImportType(String importType) {
        return list(new LambdaQueryWrapper<ImportTemplate>()
                .eq(ImportTemplate::getImportType, importType)
                .orderByDesc(ImportTemplate::getCreateTime));
    }

    /**
     * 根据模板名称获取模板（确保模板名称是全局唯一，防止混淆）
     * @param templateName
     * @return
     */
    @Override
    public ImportTemplate getByName(String templateName) {
        return getOne(new LambdaQueryWrapper<ImportTemplate>()
                .eq(ImportTemplate::getTemplateName, templateName));
    }

    /**
     * 保存导入模板
     * @param template
     * @param userId
     * @return
     */
    @Override
    public boolean saveTemplate(ImportTemplate template, Long userId) {
        template.setCreateBy(userId);
        template.setCreateTime(new Date());
        template.setUpdateTime(new Date());
        return save(template);
    }
}
