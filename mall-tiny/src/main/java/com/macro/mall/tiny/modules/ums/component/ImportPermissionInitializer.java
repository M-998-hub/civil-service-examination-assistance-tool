package com.macro.mall.tiny.modules.ums.component;

import com.macro.mall.tiny.modules.ums.model.UmsResource;
import com.macro.mall.tiny.modules.ums.model.UmsRoleResourceRelation;
import com.macro.mall.tiny.modules.ums.service.UmsAdminCacheService;
import com.macro.mall.tiny.modules.ums.service.UmsResourceService;
import com.macro.mall.tiny.modules.ums.service.UmsRoleResourceRelationService;
import com.macro.mall.tiny.modules.ums.service.UmsRoleService;
import com.macro.mall.tiny.security.component.DynamicSecurityMetadataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 导入权限初始化器
 * 系统启动时自动创建导入相关权限资源
 * Created by macro on 2026-04-03.
 */
@Component
public class ImportPermissionInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportPermissionInitializer.class);

    @Autowired
    private UmsResourceService resourceService;

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
        LOGGER.info("开始初始化导入权限资源...");
        
        // 创建导入权限资源
        createResourceIfNotExists(1001L, "ums:import:upload", "/admin/import/upload", "Excel导入上传权限");
        createResourceIfNotExists(1002L, "ums:import:execute", "/admin/import/execute", "Excel导入执行权限");
        createResourceIfNotExists(1003L, "ums:import:template", "/admin/import/templates", "导入模板管理权限");
        
        // 创建角色管理权限资源
        createResourceIfNotExists(1004L, "ums:role:list", "/role/list", "角色列表查询权限");
        createResourceIfNotExists(1005L, "ums:role:resource", "/role/resource/**", "角色资源查询权限");
        createResourceIfNotExists(1006L, "ums:role:assign", "/role/resource/assign", "角色资源分配权限");
        createResourceIfNotExists(1007L, "ums:resource:tree", "/resource/tree", "资源树查询权限");
        
        // 将权限分配给管理员角色（角色ID=9）
        assignResourcesToAdminRole();
        
        // 清除并重新加载权限数据源
        dynamicSecurityMetadataSource.clearDataSource();
        dynamicSecurityMetadataSource.loadDataSource();
        LOGGER.info("权限数据源已重新加载");
        
        LOGGER.info("导入权限资源初始化完成");
    }
    
    /**
     * 将资源权限分配给管理员角色
     */
    private void assignResourcesToAdminRole() {
        Long adminRoleId = 9L; // 管理员角色ID为9
        Long[] resourceIds = {1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L};
        
        for (Long resourceId : resourceIds) {
            // 检查是否已存在关系
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
        
        // 清除用户1的权限缓存（用户1关联管理员角色）
        cacheService.delResourceList(1L);
    }

    /**
     * 如果资源不存在则创建
     */
    private void createResourceIfNotExists(Long id, String name, String url, String description) {
        UmsResource resource = resourceService.getById(id);
        if (resource == null) {
            resource = new UmsResource();
            resource.setId(id);
            resource.setName(name);
            resource.setUrl(url);
            resource.setDescription(description);
            resource.setCreateTime(new Date());
            resource.setCategoryId(1L);
            resourceService.save(resource);
            LOGGER.info("创建权限资源: {} - {}", name, url);
        } else {
            // 更新现有资源
            resource.setName(name);
            resource.setUrl(url);
            resource.setDescription(description);
            resourceService.updateById(resource);
            LOGGER.info("更新权限资源: {} - {}", name, url);
        }
    }
}
