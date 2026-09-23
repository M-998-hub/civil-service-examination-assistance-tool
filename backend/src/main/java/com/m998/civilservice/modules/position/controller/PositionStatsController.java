package com.m998.civilservice.modules.position.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.m998.civilservice.common.api.CommonPage;
import com.m998.civilservice.common.api.CommonResult;
import com.m998.civilservice.modules.position.model.PositionStats;
import com.m998.civilservice.modules.position.service.PositionStatsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报录比/分数线表 前端控制器
 */
@Controller
@Api(tags = "PositionStatsController")
@Tag(name = "PositionStatsController", description = "报录比/分数线表")
@RequestMapping("/positionStats")
public class PositionStatsController {

    @Autowired
    private PositionStatsService positionStatsService;

    @ApiOperation(value = "添加PositionStats")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestBody PositionStats positionStats) {
        boolean success = positionStatsService.save(positionStats);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "修改PositionStats")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody PositionStats positionStats) {
        positionStats.setId(id);
        boolean success = positionStatsService.updateById(positionStats);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "删除PositionStats")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        boolean success = positionStatsService.removeById(id);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "根据ID获取PositionStats")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<PositionStats> getItem(@PathVariable Long id) {
        PositionStats positionStats = positionStatsService.getById(id);
        return CommonResult.success(positionStats);
    }

    @ApiOperation(value = "分页获取PositionStats列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<PositionStats>> list(
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        Page<PositionStats> page = positionStatsService.page(new Page<>(pageNum, pageSize));
        return CommonResult.success(CommonPage.restPage(page));
    }
}
