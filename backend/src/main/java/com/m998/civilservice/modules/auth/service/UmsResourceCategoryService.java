package com.m998.civilservice.modules.auth.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.m998.civilservice.modules.auth.model.UmsResourceCategory;

import java.util.List;

/**
 * 后台资源分类管理Service
 */
public interface UmsResourceCategoryService extends IService<UmsResourceCategory> {

    /**
     * 获取所有资源分类
     */
    List<UmsResourceCategory> listAll();

    /**
     * 创建资源分类
     */
    boolean create(UmsResourceCategory umsResourceCategory);
}
