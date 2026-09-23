package com.m998.civilservice.modules.auth.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.m998.civilservice.modules.auth.dto.ResourceTreeDto;
import com.m998.civilservice.modules.auth.model.UmsResource;

import java.util.List;

/**
 * 后台资源管理Service
 */
public interface UmsResourceService extends IService<UmsResource> {
    /**
     * 添加资源
     */
    boolean create(UmsResource umsResource);

    /**
     * 修改资源
     */
    boolean update(Long id, UmsResource umsResource);

    /**
     * 删除资源
     */
    boolean delete(Long id);

    /**
     * 分页查询资源
     */
    Page<UmsResource> list(Long categoryId, String nameKeyword, String urlKeyword, Integer pageSize, Integer pageNum);

    /**
     * 获取资源树（按分类分组）
     */
    List<ResourceTreeDto> getResourceTree();
}
