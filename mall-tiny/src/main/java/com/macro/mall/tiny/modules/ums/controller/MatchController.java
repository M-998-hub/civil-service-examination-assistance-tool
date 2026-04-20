package com.macro.mall.tiny.modules.ums.controller;

import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.dto.MatchResultDto;
import com.macro.mall.tiny.modules.ums.model.UmsAdmin;
import com.macro.mall.tiny.modules.ums.model.UserArchive;
import com.macro.mall.tiny.modules.ums.service.MatchService;
import com.macro.mall.tiny.modules.ums.service.UmsAdminService;
import com.macro.mall.tiny.modules.ums.service.UserArchiveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

/**
 * 岗位匹配控制器
 * Created by macro on 2026-04-03.
 */
@Controller
@Api(tags = "MatchController")
@Tag(name = "MatchController", description = "岗位匹配")
@RequestMapping("/match")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private UmsAdminService adminService;

    @Autowired
    private UserArchiveService userArchiveService;

    @ApiOperation(value = "一键匹配推荐岗位")
    @RequestMapping(value = "/recommend", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<List<MatchResultDto>> recommend(Principal principal) {
        if (principal == null) {
            return CommonResult.unauthorized(null);
        }
        
        // 获取当前登录用户
        String username = principal.getName();
        UmsAdmin admin = adminService.getAdminByUsername(username);
        if (admin == null) {
            return CommonResult.unauthorized(null);
        }
        
        // 获取用户档案
        UserArchive userArchive = userArchiveService.getByUserId(admin.getId());
        if (userArchive == null) {
            return CommonResult.failed("请先完善个人档案");
        }
        
        // 执行匹配
        List<MatchResultDto> results = matchService.recommend(userArchive);
        return CommonResult.success(results);
    }
}
