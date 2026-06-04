package com.macro.mall.tiny.modules.ums.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.macro.mall.tiny.common.api.CommonPage;
import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.service.PositionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 岗位管理-管理端控制器
 */
@Controller
@Api(tags = "AdminPositionController")
@Tag(name = "AdminPositionController", description = "岗位管理-管理端")
@RequestMapping("/admin/position")
public class AdminPositionController {

    @Autowired
    private PositionService positionService;

    @ApiOperation(value = "管理端-分页获取岗位列表")
    @GetMapping("/page")
    @ResponseBody
    public CommonResult<CommonPage<Position>> adminPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "year", required = false) Integer year) {
        Page<Position> page = positionService.adminPage(pageNum, pageSize, department, year);
        return CommonResult.success(CommonPage.restPage(page));
    }

    @ApiOperation(value = "管理端-获取岗位详情")
    @GetMapping("/{id}")
    @ResponseBody
    public CommonResult<Position> adminGetItem(@PathVariable Long id) {
        Position position = positionService.getById(id);
        return CommonResult.success(position);
    }

    @ApiOperation(value = "管理端-新增岗位")
    @PostMapping("")
    @ResponseBody
    public CommonResult adminCreate(@RequestBody Position position) {
        position.setStatus(0);
        boolean success = positionService.save(position);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "管理端-编辑岗位")
    @PutMapping("/{id}")
    @ResponseBody
    public CommonResult adminUpdate(@PathVariable Long id, @RequestBody Position position) {
        position.setId(id);
        boolean success = positionService.updateById(position);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "管理端-删除岗位")
    @DeleteMapping("/{id}")
    @ResponseBody
    public CommonResult adminDelete(@PathVariable Long id) {
        boolean success = positionService.deleteById(id);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "管理端-批量删除岗位")
    @DeleteMapping("/batch")
    @ResponseBody
    public CommonResult adminBatchDelete(@RequestBody Map<String, List<Long>> param) {
        List<Long> ids = param.get("ids");
        boolean success = positionService.batchDelete(ids);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }
}
