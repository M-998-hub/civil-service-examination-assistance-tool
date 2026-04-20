package com.macro.mall.tiny.modules.ums.component;

import com.macro.mall.tiny.modules.ums.model.UmsAdminRoleRelation;
import com.macro.mall.tiny.modules.ums.model.UmsRole;
import com.macro.mall.tiny.modules.ums.service.UmsAdminService;
import com.macro.mall.tiny.modules.ums.service.UmsRoleService;
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
 * 角色初始化器
 * 系统启动时自动创建默认角色并分配给 admin 用户
 * Created by macro on 2026-04-03.
 */
@Component
public class RoleInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleInitializer.class);

    @Autowired
    private UmsRoleService roleService;

    @Autowired
    private UmsAdminService adminService;

    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("开始初始化角色数据...");
        
        // 1. 创建管理员角色
        Long adminRoleId = createRoleIfNotExists("管理员", "拥有所有权限，可管理系统所有功能", 0);
        
        // 2. 创建普通用户角色
        createRoleIfNotExists("普通用户", "普通用户，只能查看岗位和匹配", 1);
        
        // 3. 创建数据录入员角色
        createRoleIfNotExists("数据录入员", "负责岗位数据录入和管理", 2);
        
        // 4. 将管理员角色分配给 admin 用户（假设 admin 用户 id = 1）
        if (adminRoleId != null) {
            grantRoleToAdmin(1L, adminRoleId);
        }
        
        LOGGER.info("角色数据初始化完成");
    }

    /**
     * 如果角色不存在则创建
     * @param name 角色名称
     * @param description 角色描述
     * @param sort 排序
     * @return 角色ID
     */
    private Long createRoleIfNotExists(String name, String description, int sort) {
        // 查询是否已存在
        List<UmsRole> existingRoles = roleService.list();
        for (UmsRole role : existingRoles) {
            if (role.getName().equals(name)) {
                LOGGER.info("角色 '{}' 已存在，跳过创建", name);
                return role.getId();
            }
        }
        
        // 创建新角色
        UmsRole role = new UmsRole();
        role.setName(name);
        role.setDescription(description);
        role.setStatus(1);
        role.setSort(sort);
        role.setCreateTime(new Date());
        
        boolean success = roleService.save(role);
        if (success) {
            LOGGER.info("创建角色 '{}' 成功", name);
            // 重新查询获取ID
            List<UmsRole> roles = roleService.list();
            for (UmsRole r : roles) {
                if (r.getName().equals(name)) {
                    return r.getId();
                }
            }
        } else {
            LOGGER.error("创建角色 '{}' 失败", name);
        }
        return null;
    }

    /**
     * 给 admin 用户分配角色
     * @param adminId 用户ID
     * @param roleId 角色ID
     */
    private void grantRoleToAdmin(Long adminId, Long roleId) {
        // 获取用户已有的角色
        List<UmsRole> existingRoles = adminService.getRoleList(adminId);
        
        // 检查是否已存在该角色
        for (UmsRole role : existingRoles) {
            if (role.getId().equals(roleId)) {
                LOGGER.info("用户 {} 已拥有角色 {}，跳过分配", adminId, roleId);
                return;
            }
        }
        
        // 添加新角色
        List<Long> roleIds = new ArrayList<>();
        for (UmsRole role : existingRoles) {
            roleIds.add(role.getId());
        }
        roleIds.add(roleId);
        
        int count = adminService.updateRole(adminId, roleIds);
        if (count >= 0) {
            LOGGER.info("成功给用户 {} 分配角色 {}", adminId, roleId);
        } else {
            LOGGER.error("给用户 {} 分配角色 {} 失败", adminId, roleId);
        }
    }
}
