package com.macro.mall.tiny.modules.ums.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.macro.mall.tiny.modules.ums.dto.ResourceTreeDto;
import com.macro.mall.tiny.modules.ums.mapper.UmsResourceMapper;
import com.macro.mall.tiny.modules.ums.model.UmsResource;
import com.macro.mall.tiny.modules.ums.model.UmsResourceCategory;
import com.macro.mall.tiny.modules.ums.service.UmsAdminCacheService;
import com.macro.mall.tiny.modules.ums.service.UmsResourceCategoryService;
import com.macro.mall.tiny.modules.ums.service.UmsResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 后台资源管理Service实现类
 * Created by macro on 2020/2/2.
 */
@Service
public class UmsResourceServiceImpl extends ServiceImpl<UmsResourceMapper,UmsResource>implements UmsResourceService {
    @Autowired
    private UmsAdminCacheService adminCacheService;

    @Autowired
    private UmsResourceCategoryService resourceCategoryService;
    @Override
    public boolean create(UmsResource umsResource) {
        umsResource.setCreateTime(new Date());
        return save(umsResource);
    }

    @Override
    public boolean update(Long id, UmsResource umsResource) {
        umsResource.setId(id);
        boolean success = updateById(umsResource);
        adminCacheService.delResourceListByResource(id);
        return success;
    }

    @Override
    public boolean delete(Long id) {
        boolean success = removeById(id);
        adminCacheService.delResourceListByResource(id);
        return success;
    }

    @Override
    public Page<UmsResource> list(Long categoryId, String nameKeyword, String urlKeyword, Integer pageSize, Integer pageNum) {
        Page<UmsResource> page = new Page<>(pageNum,pageSize);
        QueryWrapper<UmsResource> wrapper = new QueryWrapper<>();
        LambdaQueryWrapper<UmsResource> lambda = wrapper.lambda();
        if(categoryId!=null){
            lambda.eq(UmsResource::getCategoryId,categoryId);
        }
        if(StrUtil.isNotEmpty(nameKeyword)){
            lambda.like(UmsResource::getName,nameKeyword);
        }
        if(StrUtil.isNotEmpty(urlKeyword)){
            lambda.like(UmsResource::getUrl,urlKeyword);
        }
        return page(page,wrapper);
    }

    @Override
    public List<ResourceTreeDto> getResourceTree() {
        List<ResourceTreeDto> tree = new ArrayList<>();

        // 获取所有资源分类
        List<UmsResourceCategory> categories = resourceCategoryService.listAll();

        // 获取所有资源
        List<UmsResource> allResources = list();

        // 按分类分组
        for (UmsResourceCategory category : categories) {
            ResourceTreeDto dto = new ResourceTreeDto();
            dto.setCategoryId(category.getId());
            dto.setCategoryName(category.getName());

            // 筛选该分类下的资源
            List<UmsResource> categoryResources = new ArrayList<>();
            for (UmsResource resource : allResources) {
                if (category.getId().equals(resource.getCategoryId())) {
                    categoryResources.add(resource);
                }
            }
            dto.setResources(categoryResources);
            tree.add(dto);
        }

        return tree;
    }
}
