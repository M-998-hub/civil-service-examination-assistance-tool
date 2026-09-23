package com.m998.civilservice.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.m998.civilservice.modules.auth.mapper.UmsResourceCategoryMapper;
import com.m998.civilservice.modules.auth.model.UmsResourceCategory;
import com.m998.civilservice.modules.auth.service.UmsResourceCategoryService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 后台资源分类管理Service实现类
 *
 * 功能：管理权限资源的分类
 */
@Service
public class UmsResourceCategoryServiceImpl extends ServiceImpl<UmsResourceCategoryMapper,UmsResourceCategory> implements UmsResourceCategoryService {

    /**
     * 获取所有资源分类列表
     * @return
     */
    @Override
    public List<UmsResourceCategory> listAll() {
        // 创建查询包装器
        QueryWrapper<UmsResourceCategory> wrapper = new QueryWrapper<>();
        // 按排序号降序排序
        wrapper.lambda().orderByDesc(UmsResourceCategory::getSort);
        // 执行查询，返回所有分类
        return list(wrapper);
    }

    /**
     * 创建资源分类
     * @param umsResourceCategory
     * @return
     */
    @Override
    public boolean create(UmsResourceCategory umsResourceCategory) {
        // 设置创建时间为当前时间
        umsResourceCategory.setCreateTime(new Date());
        // 调用 MyBatis-Plus 的 save 方法保存
        return save(umsResourceCategory);
    }
}
