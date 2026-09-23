package com.m998.civilservice.modules.ai.controller;

import com.m998.civilservice.common.api.CommonResult;
import com.m998.civilservice.modules.ai.dto.AiRequest;
import com.m998.civilservice.modules.ai.dto.AiResponse;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.auth.model.UmsAdmin;
import com.m998.civilservice.modules.profile.model.UserArchive;
import com.m998.civilservice.modules.ai.service.AiService;
import com.m998.civilservice.modules.position.service.PositionService;
import com.m998.civilservice.modules.auth.service.UmsAdminService;
import com.m998.civilservice.modules.profile.service.UserArchiveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@Api(tags = "AiController")
@Tag(name = "AiController", description = "AI辅助功能")
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;
    @Autowired
    private UmsAdminService adminService;
    @Autowired
    private UserArchiveService userArchiveService;
    @Autowired
    private PositionService positionService;

    @ApiOperation(value = "AI分析岗位匹配")
    @RequestMapping(value = "/analyze", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<AiResponse> analyze(Principal principal, @RequestBody AiRequest request) {
        if (principal == null) return CommonResult.unauthorized(null);
        if (request.getApiKey() == null || request.getApiKey().isEmpty())
            return CommonResult.validateFailed("API Key不能为空");
        if (request.getPositionId() == null)
            return CommonResult.validateFailed("岗位ID不能为空");

        UmsAdmin admin = adminService.getAdminByUsername(principal.getName());
        if (admin == null) return CommonResult.unauthorized(null);

        UserArchive archive = userArchiveService.getByUserId(admin.getId());
        Position position = positionService.getById(request.getPositionId());
        if (position == null) return CommonResult.failed("岗位不存在");

        AiResponse resp = aiService.analyzeMatch(request.getApiKey(), archive, position, request.getMatchScore(), request.getMatchDetails());
        return CommonResult.success(resp);
    }

    @ApiOperation(value = "AI智能问答")
    @RequestMapping(value = "/ask", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<AiResponse> ask(Principal principal, @RequestBody AiRequest request) {
        if (principal == null) return CommonResult.unauthorized(null);
        if (request.getApiKey() == null || request.getApiKey().isEmpty())
            return CommonResult.validateFailed("API Key不能为空");
        if (request.getQuestion() == null || request.getQuestion().isEmpty())
            return CommonResult.validateFailed("问题不能为空");

        UmsAdmin admin = adminService.getAdminByUsername(principal.getName());
        if (admin == null) return CommonResult.unauthorized(null);

        UserArchive archive = userArchiveService.getByUserId(admin.getId());
        AiResponse resp = aiService.ask(request.getApiKey(), request.getQuestion(), archive);
        return CommonResult.success(resp);
    }
}
