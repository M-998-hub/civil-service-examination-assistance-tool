package com.m998.civilservice.modules.match.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.m998.civilservice.common.api.CommonPage;
import com.m998.civilservice.common.api.CommonResult;
import com.m998.civilservice.modules.match.model.PredictionLog;
import com.m998.civilservice.modules.match.service.PredictionLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预测记录表 前端控制器
 */
@Controller
@Api(tags = "PredictionLogController")
@Tag(name = "PredictionLogController", description = "预测记录表")
@RequestMapping("/predictionLog")
public class PredictionLogController {

    @Autowired
    private PredictionLogService predictionLogService;

    @ApiOperation(value = "添加PredictionLog")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestBody PredictionLog predictionLog) {
        boolean success = predictionLogService.save(predictionLog);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "修改PredictionLog")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody PredictionLog predictionLog) {
        predictionLog.setId(id);
        boolean success = predictionLogService.updateById(predictionLog);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "删除PredictionLog")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        boolean success = predictionLogService.removeById(id);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "根据ID获取PredictionLog")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<PredictionLog> getItem(@PathVariable Long id) {
        PredictionLog predictionLog = predictionLogService.getById(id);
        return CommonResult.success(predictionLog);
    }

    @ApiOperation(value = "分页获取PredictionLog列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<PredictionLog>> list(
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        Page<PredictionLog> page = predictionLogService.page(new Page<>(pageNum, pageSize));
        return CommonResult.success(CommonPage.restPage(page));
    }
}
