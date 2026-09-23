package com.m998.civilservice.modules.match.controller;

import com.m998.civilservice.common.api.CommonResult;
import com.m998.civilservice.modules.match.dto.MatchResultDto;
import com.m998.civilservice.modules.auth.model.UmsAdmin;
import com.m998.civilservice.modules.profile.model.UserArchive;
import com.m998.civilservice.modules.match.service.MatchService;
import com.m998.civilservice.modules.auth.service.UmsAdminService;
import com.m998.civilservice.modules.profile.service.UserArchiveService;
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
