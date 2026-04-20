package com.macro.mall.tiny.modules.ums.controller;

import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.dto.*;
import com.macro.mall.tiny.modules.ums.model.ImportTemplate;
import com.macro.mall.tiny.modules.ums.service.AdminImportService;
import com.macro.mall.tiny.modules.ums.service.ImportTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端数据导入控制器
 * Created by macro on 2026-04-03.
 */
@Controller
@Api(tags = "AdminImportController")
@Tag(name = "AdminImportController", description = "管理端数据导入")
@RequestMapping("/admin/import")
public class AdminImportController {

    @Autowired
    private AdminImportService adminImportService;

    @Autowired
    private ImportTemplateService templateService;

    @ApiOperation(value = "上传Excel并预览")
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("hasAuthority('ums:import:upload')")
    public CommonResult<UploadResult> upload(@RequestParam("file") MultipartFile file) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return CommonResult.failed("上传文件不能为空");
            }
            
            // 检查文件格式
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return CommonResult.failed("文件格式不正确，请上传Excel文件（.xlsx或.xls）");
            }
            
            UploadResult result = adminImportService.uploadAndPreview(file);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("上传失败: " + e.getMessage());
        }
    }

    @ApiOperation(value = "执行导入")
    @RequestMapping(value = "/execute", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("hasAuthority('ums:import:execute')")
    public CommonResult<ExecuteImportResult> execute(
            @RequestBody ExecuteImportParam param,
            Principal principal) {
        try {
            // 获取当前用户ID（可根据实际需要从principal获取）
            Long userId = null;
            
            ExecuteImportResult result = adminImportService.executeImport(param, userId);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("导入失败: " + e.getMessage());
        }
    }

    @ApiOperation(value = "获取模板列表")
    @RequestMapping(value = "/templates", method = RequestMethod.GET)
    @ResponseBody
    @PreAuthorize("hasAuthority('ums:import:template')")
    public CommonResult<List<TemplateDto>> listTemplates() {
        List<ImportTemplate> templates = templateService.listAll();
        List<TemplateDto> dtoList = templates.stream().map(t -> {
            TemplateDto dto = new TemplateDto();
            BeanUtils.copyProperties(t, dto);
            return dto;
        }).collect(Collectors.toList());
        return CommonResult.success(dtoList);
    }

    @ApiOperation(value = "删除模板")
    @RequestMapping(value = "/template/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @PreAuthorize("hasAuthority('ums:import:template')")
    public CommonResult deleteTemplate(@PathVariable Long id) {
        boolean success = templateService.removeById(id);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed("删除失败");
    }
}
