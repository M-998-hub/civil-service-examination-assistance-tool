package com.macro.mall.tiny.modules.ums.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.macro.mall.tiny.common.api.CommonPage;
import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.dto.PositionFilterParam;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.service.PositionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位表 前端控制器
 * Created by macro on 2026-04-10.
 */
@Controller
@Api(tags = "PositionController")
@Tag(name = "PositionController", description = "岗位表")
@RequestMapping("/position")
public class PositionController {

    @Autowired
    private PositionService positionService;

    @ApiOperation(value = "添加Position")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestBody Position position) {
        boolean success = positionService.save(position);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "修改Position")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody Position position) {
        position.setId(id);
        boolean success = positionService.updateById(position);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "删除Position")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        boolean success = positionService.removeById(id);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "根据ID获取Position")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<Position> getItem(@PathVariable Long id) {
        Position position = positionService.getById(id);
        return CommonResult.success(position);
    }

    @ApiOperation(value = "分页获取Position列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<Position>> list(
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        Page<Position> page = positionService.page(new Page<>(pageNum, pageSize));
        return CommonResult.success(CommonPage.restPage(page));
    }

    @ApiOperation(value = "根据条件筛选岗位")
    @RequestMapping(value = "/filter", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<Page<Position>> filter(@RequestBody PositionFilterParam param) {
        Page<Position> page = positionService.filter(param);
        return CommonResult.success(page);
    }
}
