package com.macro.mall.tiny.modules.ums.controller;

import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.model.UmsResource;
import com.macro.mall.tiny.modules.ums.model.UmsRole;
import com.macro.mall.tiny.modules.ums.service.UmsAdminService;
import com.macro.mall.tiny.modules.ums.service.UmsRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 角色管理控制器
 * Created by macro on 2026-04-03.
 */
@Controller
@Api(tags = "UmsRoleController")
@Tag(name = "UmsRoleController", description = "角色管理")
@RequestMapping("/role")
public class UmsRoleController {

    @Autowired
    private UmsRoleService roleService;

    @Autowired
    private UmsAdminService adminService;

    @ApiOperation(value = "创建角色")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestBody UmsRole role) {
        boolean success = roleService.create(role);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "给用户分配角色")
    @RequestMapping(value = "/grant", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult grant(@RequestBody GrantRoleParam param) {
        // 使用现有的 updateRole 方法
        List<Long> roleIds = new ArrayList<>();
        // 先获取用户已有的角色
        List<UmsRole> existingRoles = adminService.getRoleList(param.getAdminId());
        for (UmsRole role : existingRoles) {
            roleIds.add(role.getId());
        }
        // 添加新角色（如果不存在）
        if (!roleIds.contains(param.getRoleId())) {
            roleIds.add(param.getRoleId());
        }
        int count = adminService.updateRole(param.getAdminId(), roleIds);
        if (count >= 0) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "查询用户的角色列表")
    @RequestMapping(value = "/user/{adminId}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<UmsRole>> getUserRoles(@PathVariable Long adminId) {
        List<UmsRole> roles = adminService.getRoleList(adminId);
        return CommonResult.success(roles);
    }

    @ApiOperation(value = "撤销用户角色")
    @RequestMapping(value = "/revoke", method = RequestMethod.DELETE)
    @ResponseBody
    public CommonResult revoke(@RequestBody GrantRoleParam param) {
        // 获取用户已有的角色
        List<UmsRole> existingRoles = adminService.getRoleList(param.getAdminId());
        List<Long> roleIds = new ArrayList<>();
        for (UmsRole role : existingRoles) {
            // 排除要撤销的角色
            if (!role.getId().equals(param.getRoleId())) {
                roleIds.add(role.getId());
            }
        }
        int count = adminService.updateRole(param.getAdminId(), roleIds);
        if (count >= 0) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "更新角色")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody UmsRole role) {
        role.setId(id);
        boolean success = roleService.updateById(role);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "删除角色")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        boolean success = roleService.delete(Collections.singletonList(id));
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "查询所有角色")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<UmsRole>> list() {
        List<UmsRole> roles = roleService.list();
        return CommonResult.success(roles);
    }

    @ApiOperation(value = "获取角色已拥有的资源ID列表")
    @RequestMapping(value = "/resource/{roleId}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<Long>> getRoleResources(@PathVariable Long roleId) {
        List<UmsResource> resources = roleService.listResource(roleId);
        List<Long> resourceIds = new ArrayList<>();
        for (UmsResource resource : resources) {
            resourceIds.add(resource.getId());
        }
        return CommonResult.success(resourceIds);
    }

    @ApiOperation(value = "给角色分配资源")
    @RequestMapping(value = "/resource/assign", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult assignResources(@RequestBody AssignResourceParam param) {
        int count = roleService.allocResource(param.getRoleId(), param.getResourceIds());
        if (count >= 0) {
            return CommonResult.success(count);
        }
        return CommonResult.failed();
    }

    /**
     * 分配角色参数
     */
    public static class GrantRoleParam {
        private Long adminId;
        private Long roleId;

        public Long getAdminId() {
            return adminId;
        }

        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }
    }

    /**
     * 分配资源参数
     */
    public static class AssignResourceParam {
        private Long roleId;
        private List<Long> resourceIds;

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public List<Long> getResourceIds() {
            return resourceIds;
        }

        public void setResourceIds(List<Long> resourceIds) {
            this.resourceIds = resourceIds;
        }
    }
}
