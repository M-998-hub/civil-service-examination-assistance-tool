package com.macro.mall.tiny.modules.ums.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.macro.mall.tiny.common.api.CommonPage;
import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.model.UserArchive;
import com.macro.mall.tiny.modules.ums.service.UserArchiveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户档案表 前端控制器
 * Created by macro on 2026-04-10.
 */
@Controller
@Api(tags = "UserArchiveController")
@Tag(name = "UserArchiveController", description = "用户档案表")
@RequestMapping("/user/archive")
public class UserArchiveController {

    @Autowired
    private UserArchiveService userArchiveService;

    @ApiOperation(value = "添加UserArchive")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestBody UserArchive userArchive) {
        boolean success = userArchiveService.save(userArchive);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "修改UserArchive")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody UserArchive userArchive) {
        userArchive.setId(id);
        boolean success = userArchiveService.updateById(userArchive);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "删除UserArchive")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        boolean success = userArchiveService.removeById(id);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "根据ID获取UserArchive")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<UserArchive> getItem(@PathVariable Long id) {
        UserArchive userArchive = userArchiveService.getById(id);
        return CommonResult.success(userArchive);
    }

    @ApiOperation(value = "分页获取UserArchive列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<UserArchive>> list(
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        Page<UserArchive> page = userArchiveService.page(new Page<>(pageNum, pageSize));
        return CommonResult.success(CommonPage.restPage(page));
    }

    @ApiOperation(value = "获取当前用户档案")
    @RequestMapping(value = "/my", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<UserArchive> getMyArchive() {
        UserArchive userArchive = userArchiveService.getMyArchive();
        if (userArchive == null) {
            return CommonResult.success(null);
        }
        return CommonResult.success(userArchive);
    }

    @ApiOperation(value = "保存当前用户档案")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult saveMyArchive(@RequestBody UserArchive userArchive) {
        boolean success = userArchiveService.saveMyArchive(userArchive);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed("保存失败，请检查登录状态");
    }
}
