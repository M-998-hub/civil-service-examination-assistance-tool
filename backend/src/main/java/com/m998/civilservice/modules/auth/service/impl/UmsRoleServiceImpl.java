package com.m998.civilservice.modules.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.m998.civilservice.modules.auth.mapper.UmsMenuMapper;
import com.m998.civilservice.modules.auth.mapper.UmsResourceMapper;
import com.m998.civilservice.modules.auth.mapper.UmsRoleMapper;
import com.m998.civilservice.modules.auth.model.*;
import com.m998.civilservice.modules.auth.service.UmsAdminCacheService;
import com.m998.civilservice.modules.auth.service.UmsRoleMenuRelationService;
import com.m998.civilservice.modules.auth.service.UmsRoleResourceRelationService;
import com.m998.civilservice.modules.auth.service.UmsRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 后台角色管理Service实现类
 */
@Service
public class UmsRoleServiceImpl extends ServiceImpl<UmsRoleMapper,UmsRole>implements UmsRoleService {
    // 角色缓存服务。用于在角色权限变更时清理相关用户的缓存
    @Autowired
    private UmsAdminCacheService adminCacheService;
    @Autowired
    private UmsRoleMenuRelationService roleMenuRelationService;
    @Autowired
    private UmsRoleResourceRelationService roleResourceRelationService;
    @Autowired
    private UmsMenuMapper menuMapper;
    @Autowired
    private UmsResourceMapper resourceMapper;
    @Override
    public boolean create(UmsRole role) {
        role.setCreateTime(new Date());
        role.setAdminCount(0);
        role.setSort(0);
        return save(role);
    }

    @Override
    public boolean delete(List<Long> ids) {
        boolean success = removeByIds(ids);
        adminCacheService.delResourceListByRoleIds(ids);
        return success;
    }

    @Override
    public Page<UmsRole> list(String keyword, Integer pageSize, Integer pageNum) {
        Page<UmsRole> page = new Page<>(pageNum,pageSize);
        QueryWrapper<UmsRole> wrapper = new QueryWrapper<>();
        LambdaQueryWrapper<UmsRole> lambda = wrapper.lambda();
        if(StrUtil.isNotEmpty(keyword)){
            lambda.like(UmsRole::getName,keyword);
        }
        return page(page,wrapper);
    }

    @Override
    public List<UmsMenu> getMenuList(Long adminId) {
        return menuMapper.getMenuList(adminId);
    }

    @Override
    public List<UmsMenu> listMenu(Long roleId) {
        return menuMapper.getMenuListByRoleId(roleId);
    }

    @Override
    public List<UmsResource> listResource(Long roleId) {
        return resourceMapper.getResourceListByRoleId(roleId);
    }

    /**
     * 为角色分配菜单权限
     * @param roleId
     * @param menuIds
     * @return
     */
    @Override
    public int allocMenu(Long roleId, List<Long> menuIds) {
        //先删除原有关系
        QueryWrapper<UmsRoleMenuRelation> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsRoleMenuRelation::getRoleId,roleId);
        roleMenuRelationService.remove(wrapper);
        //批量插入新关系
        List<UmsRoleMenuRelation> relationList = new ArrayList<>();
        for (Long menuId : menuIds) {
            UmsRoleMenuRelation relation = new UmsRoleMenuRelation();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            relationList.add(relation);
        }
        roleMenuRelationService.saveBatch(relationList);
        return menuIds.size();
    }

    /**
     * 为角色分配资源权限
     * @param roleId
     * @param resourceIds
     * @return
     */
    @Override
    public int allocResource(Long roleId, List<Long> resourceIds) {
        //先删除原有关系
        QueryWrapper<UmsRoleResourceRelation> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsRoleResourceRelation::getRoleId,roleId);
        roleResourceRelationService.remove(wrapper);
        //批量插入新关系
        List<UmsRoleResourceRelation> relationList = new ArrayList<>();
        for (Long resourceId : resourceIds) {
            UmsRoleResourceRelation relation = new UmsRoleResourceRelation();
            relation.setRoleId(roleId);
            relation.setResourceId(resourceId);
            relationList.add(relation);
        }
        roleResourceRelationService.saveBatch(relationList);

        // 清理缓存（拥有该角色的所有用户的资源缓存）
        adminCacheService.delResourceListByRole(roleId);
        return resourceIds.size();
    }
}
