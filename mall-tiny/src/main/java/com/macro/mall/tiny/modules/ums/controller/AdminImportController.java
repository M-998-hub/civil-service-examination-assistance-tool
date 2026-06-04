package com.macro.mall.tiny.modules.ums.controller;

import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.dto.*;
import com.macro.mall.tiny.modules.ums.model.ImportTemplate;
import com.macro.mall.tiny.modules.ums.dto.SaveTemplateParam;
import com.macro.mall.tiny.modules.ums.service.AdminImportService;
import com.macro.mall.tiny.modules.ums.service.ImportTemplateService;
import com.macro.mall.tiny.modules.ums.service.impl.AdminImportServiceImpl;
import com.macro.mall.tiny.modules.ums.strategy.ImportStrategy;
import com.macro.mall.tiny.modules.ums.strategy.ImportTypeEnum;
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
import java.util.Map;
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
    private AdminImportServiceImpl adminImportService;

    @Autowired
    private ImportTemplateService templateService;

    @ApiOperation(value = "获取支持的导入类型列表")
    @RequestMapping(value = "/types", method = RequestMethod.GET)
    @ResponseBody
    @PreAuthorize("hasAuthority('导入模板管理')")
    public CommonResult<List<Map<String, String>>> getImportTypes() {
        return CommonResult.success(adminImportService.getSupportedTypes());
    }

    @ApiOperation(value = "获取指定导入类型的字段元数据")
    @RequestMapping(value = "/fields", method = RequestMethod.GET)
    @ResponseBody
    @PreAuthorize("hasAuthority('导入模板管理')")
    public CommonResult<List<ImportFieldMeta>> getFields(@RequestParam String type) {
        ImportTypeEnum importType = ImportTypeEnum.fromCode(type);
        ImportStrategy<?> strategy = adminImportService.getStrategy(importType);
        return CommonResult.success(strategy.getFieldMetas());
    }

    @ApiOperation(value = "上传Excel并预览")
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("hasAuthority('导入模板管理')")
    public CommonResult<UploadResult> upload(@RequestParam("file") MultipartFile file) throws Exception {
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
    }

    @ApiOperation(value = "执行导入")
    @RequestMapping(value = "/execute", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("hasAuthority('Excel导入执行')")
    public CommonResult<ExecuteImportResult> execute(
            @RequestBody ExecuteImportParam param,
            Principal principal) throws Exception {
        // 获取当前用户ID（可根据实际需要从principal获取）
        Long userId = null;
        
        ExecuteImportResult result = adminImportService.executeImport(param, userId);
        return CommonResult.success(result);
    }

    @ApiOperation(value = "获取模板列表")
    @RequestMapping(value = "/templates", method = RequestMethod.GET)
    @ResponseBody
    @PreAuthorize("hasAuthority('导入模板管理')")
    public CommonResult<List<TemplateDto>> listTemplates(
            @RequestParam(required = false) String importType) {
        List<ImportTemplate> templates;
        if (importType != null && !importType.isEmpty()) {
            templates = templateService.listByImportType(importType);
        } else {
            templates = templateService.listAll();
        }
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

    @ApiOperation(value = "保存模板")
    @RequestMapping(value = "/template", method = RequestMethod.POST)
    @ResponseBody
    @PreAuthorize("hasAuthority('ums:import:template')")
    public CommonResult saveTemplate(@RequestBody SaveTemplateParam param, Principal principal) {
        ImportTemplate template = new ImportTemplate();
        template.setTemplateName(param.getTemplateName());
        template.setDescription(param.getDescription());
        template.setColumnMapping(param.getColumnMapping());
        template.setImportType(param.getImportType());

        // 获取当前用户ID
        Long userId = null;

        boolean success = templateService.saveTemplate(template, userId);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed("保存失败");
    }
}
