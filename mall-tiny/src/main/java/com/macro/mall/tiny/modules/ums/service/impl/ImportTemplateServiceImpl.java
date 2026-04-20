package com.macro.mall.tiny.modules.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.macro.mall.tiny.modules.ums.mapper.ImportTemplateMapper;
import com.macro.mall.tiny.modules.ums.model.ImportTemplate;
import com.macro.mall.tiny.modules.ums.service.ImportTemplateService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 导入模板服务实现
 * Created by macro on 2026-04-03.
 */
@Service
public class ImportTemplateServiceImpl extends ServiceImpl<ImportTemplateMapper, ImportTemplate> implements ImportTemplateService {

    @Override
    public List<ImportTemplate> listAll() {
        return list(new LambdaQueryWrapper<ImportTemplate>()
                .orderByDesc(ImportTemplate::getCreateTime));
    }

    @Override
    public ImportTemplate getByName(String templateName) {
        return getOne(new LambdaQueryWrapper<ImportTemplate>()
                .eq(ImportTemplate::getTemplateName, templateName));
    }

    @Override
    public boolean saveTemplate(ImportTemplate template, Long userId) {
        template.setCreateBy(userId);
        template.setCreateTime(new Date());
        template.setUpdateTime(new Date());
        return save(template);
    }
}
