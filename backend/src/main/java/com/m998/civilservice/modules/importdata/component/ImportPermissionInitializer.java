package com.m998.civilservice.modules.importdata.component;

import com.m998.civilservice.modules.auth.model.UmsResource;
import com.m998.civilservice.modules.auth.model.UmsResourceCategory;
import com.m998.civilservice.modules.auth.model.UmsRoleResourceRelation;
import com.m998.civilservice.modules.auth.service.UmsAdminCacheService;
import com.m998.civilservice.modules.auth.service.UmsResourceCategoryService;
import com.m998.civilservice.modules.auth.service.UmsResourceService;
import com.m998.civilservice.modules.auth.service.UmsRoleResourceRelationService;
import com.m998.civilservice.modules.auth.service.UmsRoleService;
import com.m998.civilservice.security.component.DynamicSecurityMetadataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ImportPermissionInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportPermissionInitializer.class);

    @Autowired
    private UmsResourceService resourceService;

    @Autowired
    private UmsResourceCategoryService resourceCategoryService;

    @Autowired
    private UmsRoleService roleService;

    @Autowired
    private UmsRoleResourceRelationService roleResourceRelationService;

    @Autowired
    private UmsAdminCacheService cacheService;

    @Autowired
    private DynamicSecurityMetadataSource dynamicSecurityMetadataSource;

    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("开始初始化权限资源...");

        // 创建资源分类
        createCategoryIfNotExists(5L, "用户管理", 5);
        createCategoryIfNotExists(6L, "岗位管理", 6);

        // 创建导入权限资源（分类：数据导入）
        createResourceIfNotExists(1001L, "Excel导入上传", "/admin/import/upload", "Excel导入上传权限", 1L);
        createResourceIfNotExists(1002L, "Excel导入执行", "/admin/import/execute", "Excel导入执行权限", 1L);
        createResourceIfNotExists(1003L, "导入模板管理", "/admin/import/templates", "导入模板管理权限", 1L);
        createResourceIfNotExists(1008L, "导入类型查询", "/admin/import/types", "获取支持的导入类型列表", 1L);
        createResourceIfNotExists(1009L, "导入字段查询", "/admin/import/fields", "获取导入字段元数据", 1L);

        // 创建用户管理权限资源（分类：用户管理）
        createResourceIfNotExists(1018L, "导入模板管理", "/admin/import/template/**", "模板保存与删除权限", 1L);
        createResourceIfNotExists(1010L, "用户列表", "/admin/list", "查看用户列表", 5L);
        createResourceIfNotExists(1011L, "用户编辑", "/admin/update/**", "编辑用户信息", 5L);
        createResourceIfNotExists(1012L, "用户状态管理", "/admin/updateStatus/**", "启用/禁用用户", 5L);
        createResourceIfNotExists(1013L, "用户删除", "/admin/delete/**", "删除用户", 5L);
        createResourceIfNotExists(1014L, "用户角色查看", "/admin/role/*", "查看用户拥有的角色", 5L);
        createResourceIfNotExists(1015L, "用户角色分配", "/admin/role/update", "为用户分配角色", 5L);
        
        // 创建岗位管理权限资源（分类：岗位管理）
        createResourceIfNotExists(1016L, "岗位列表查询", "/admin/position/page", "分页查询岗位", 6L);
        createResourceIfNotExists(1017L, "岗位管理操作", "/admin/position/**", "新增/编辑/删除岗位", 6L);
        
        // 创建角色/权限管理资源（分类：权限管理）
        createResourceIfNotExists(1004L, "角色列表查询", "/role/list", "角色列表查询权限", 4L);
        createResourceIfNotExists(1005L, "角色资源查询", "/role/resource/**", "角色资源查询权限", 4L);
        createResourceIfNotExists(1006L, "角色资源分配", "/role/resource/assign", "角色资源分配权限", 4L);
        createResourceIfNotExists(1007L, "资源树查询", "/resource/tree", "资源树查询权限", 4L);
        
        // 将权限分配给管理员角色（角色ID=9）
        assignResourcesToAdminRole();
        
        // 清除并重新加载权限数据源
        dynamicSecurityMetadataSource.clearDataSource();
        dynamicSecurityMetadataSource.loadDataSource();
        LOGGER.info("权限数据源已重新加载");
        
        LOGGER.info("权限资源初始化完成");
    }
    
    /**
     * 将资源权限分配给管理员角色
     */
    private void assignResourcesToAdminRole() {
        Long adminRoleId = 9L; // 管理员角色ID为9
        Long[] resourceIds = {1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L, 1018L, 1010L, 1011L, 1012L, 1013L, 1014L, 1015L, 1016L, 1017L};
        
        for (Long resourceId : resourceIds) {
            UmsRoleResourceRelation relation = roleResourceRelationService.lambdaQuery()
                    .eq(UmsRoleResourceRelation::getRoleId, adminRoleId)
                    .eq(UmsRoleResourceRelation::getResourceId, resourceId)
                    .one();
            
            if (relation == null) {
                relation = new UmsRoleResourceRelation();
                relation.setRoleId(adminRoleId);
                relation.setResourceId(resourceId);
                roleResourceRelationService.save(relation);
                LOGGER.info("将资源 {} 分配给管理员角色", resourceId);
            }
        }
        
        cacheService.delResourceList(1L);
    }

    /**
     * 如果资源不存在则创建
     */
    private void createResourceIfNotExists(Long id, String name, String url, String description, Long categoryId) {
        UmsResource resource = resourceService.getById(id);
        if (resource == null) {
            resource = new UmsResource();
            resource.setId(id);
            resource.setName(name);
            resource.setUrl(url);
            resource.setDescription(description);
            resource.setCreateTime(new Date());
            resource.setCategoryId(categoryId);
            resourceService.save(resource);
            LOGGER.info("创建权限资源: {} - {}", name, url);
        } else {
            resource.setName(name);
            resource.setUrl(url);
            resource.setDescription(description);
            resource.setCategoryId(categoryId);
            resourceService.updateById(resource);
            LOGGER.info("更新权限资源: {} - {}", name, url);
        }
    }

    /**
     * 如果资源分类不存在则创建
     */
    private void createCategoryIfNotExists(Long id, String name, Integer sort) {
        UmsResourceCategory category = resourceCategoryService.getById(id);
        if (category == null) {
            category = new UmsResourceCategory();
            category.setId(id);
            category.setName(name);
            category.setSort(sort);
            category.setCreateTime(new Date());
            resourceCategoryService.save(category);
            LOGGER.info("创建资源分类: {} (ID={})", name, id);
        }
    }
}
