package com.m998.civilservice.modules.auth.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.m998.civilservice.common.api.CommonPage;
import com.m998.civilservice.common.api.CommonResult;
import com.m998.civilservice.common.api.ResultCode;
import com.m998.civilservice.common.service.RedisService;
import com.m998.civilservice.security.util.JwtTokenUtil;
import com.m998.civilservice.modules.auth.dto.UmsAdminLoginParam;
import com.m998.civilservice.modules.auth.dto.UmsAdminParam;
import com.m998.civilservice.modules.auth.dto.UpdateAdminPasswordParam;
import com.m998.civilservice.modules.auth.model.UmsAdmin;
import com.m998.civilservice.modules.auth.model.UmsResource;
import com.m998.civilservice.modules.auth.model.UmsRole;
import com.m998.civilservice.modules.auth.service.UmsAdminService;
import com.m998.civilservice.modules.auth.service.UmsRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 后台用户管理
 */
@Controller
@Api(tags = "UmsAdminController")
@Tag(name = "UmsAdminController", description = "后台用户管理")
@RequestMapping("/admin")
public class UmsAdminController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAdminController.class);
    @Value("${jwt.tokenHeader}")
    private String tokenHeader;
    @Value("${jwt.tokenHead}")
    private String tokenHead;
    @Value("${verification.expose-code:false}")
    private boolean exposeVerificationCode;
    @Autowired
    private UmsAdminService adminService;
    @Autowired
    private UmsRoleService roleService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @ApiOperation(value = "获取图片验证码")
    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult getCaptcha() {
        // 生成线段干扰验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 20);
        String uuid = UUID.randomUUID().toString();
        String code = lineCaptcha.getCode();
        // 存入 Redis，过期 5 分钟
        String redisKey = "captcha:" + uuid;
        redisService.set(redisKey, code, 5 * 60);
        // 返回 Base64 图片
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        lineCaptcha.write(baos);
        String imageBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("imageBase64", imageBase64);
        return CommonResult.success(result);
    }

    @ApiOperation(value = "校验图片验证码")
    private boolean validateCaptcha(String captchaUuid, String captchaCode) {
        if (captchaUuid == null || captchaUuid.isEmpty() || captchaCode == null || captchaCode.isEmpty()) {
            return false;
        }
        String redisKey = "captcha:" + captchaUuid;
        String storedCode = (String) redisService.get(redisKey);
        if (storedCode == null) {
            return false;
        }
        boolean valid = storedCode.equalsIgnoreCase(captchaCode.trim());
        // 校验后立即删除，防止重复使用
        redisService.del(redisKey);
        return valid;
    }

    @ApiOperation(value = "用户注册")
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult register(@RequestBody Map<String, String> param) {
        String username = param.get("username");
        String password = param.get("password");
        String confirmPassword = param.get("confirmPassword");
        String nickName = param.get("nickName");
        String email = param.get("email");
        String captchaUuid = param.get("captchaUuid");
        String captchaCode = param.get("captchaCode");
        String emailCode = param.get("emailCode");
        // 校验参数
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return CommonResult.validateFailed("用户名和密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            return CommonResult.failed(ResultCode.PASSWORD_CONFIRM_MISMATCH);
        }
        // 校验图片验证码
        if (!validateCaptcha(captchaUuid, captchaCode)) {
            return CommonResult.validateFailed("图片验证码错误或已过期");
        }
        // 校验邮箱验证码
        if (email == null || email.trim().isEmpty()) {
            return CommonResult.validateFailed("邮箱不能为空");
        }
        if (emailCode == null || emailCode.trim().isEmpty()) {
            return CommonResult.validateFailed("邮箱验证码不能为空");
        }
        String emailCodeKey = "register:code:" + email.trim();
        String storedEmailCode = (String) redisService.get(emailCodeKey);
        if (storedEmailCode == null || !storedEmailCode.equals(emailCode.trim())) {
            return CommonResult.validateFailed("邮箱验证码错误或已过期");
        }
        redisService.del(emailCodeKey);
        // 检查用户名是否已存在
        UmsAdmin existingAdmin = adminService.getAdminByUsername(username.trim());
        if (existingAdmin != null) {
            return CommonResult.failed(ResultCode.USER_DUPLICATE);
        }
        // 创建用户
        UmsAdminParam umsAdminParam = new UmsAdminParam();
        umsAdminParam.setUsername(username.trim());
        umsAdminParam.setPassword(password);
        umsAdminParam.setNickName(nickName);
        umsAdminParam.setEmail(email);
        UmsAdmin umsAdmin = adminService.register(umsAdminParam);
        if (umsAdmin == null) {
            return CommonResult.failed("注册失败");
        }
        return CommonResult.success(null, "注册成功");
    }

    @ApiOperation(value = "注册-发送邮箱验证码")
    @RequestMapping(value = "/register/send-code", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult sendRegisterCode(@RequestBody Map<String, String> param) {
        String email = param.get("email");
        String captchaUuid = param.get("captchaUuid");
        String captchaCode = param.get("captchaCode");
        if (email == null || email.trim().isEmpty()) {
            return CommonResult.validateFailed("邮箱不能为空");
        }
        // 校验图片验证码
        if (!validateCaptcha(captchaUuid, captchaCode)) {
            return CommonResult.validateFailed("图片验证码错误或已过期");
        }
        // 生成 6 位随机验证码
        String code = RandomUtil.randomNumbers(6);
        String redisKey = "register:code:" + email.trim();
        redisService.set(redisKey, code, 5 * 60);
        LOGGER.info("注册验证码已生成，邮箱: {}", maskEmail(email));
        return CommonResult.success(buildVerificationResult(code), "验证码已发送");
    }

    @ApiOperation(value = "忘记密码-发送验证码")
    @RequestMapping(value = "/forgot/send-code", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult sendVerifyCode(@RequestBody Map<String, String> param) {
        String email = param.get("email");
        if (email == null || email.trim().isEmpty()) {
            return CommonResult.validateFailed("邮箱不能为空");
        }
        // 检查邮箱是否绑定用户
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdmin::getEmail, email.trim());
        List<UmsAdmin> adminList = adminService.list(wrapper);
        if (adminList == null || adminList.isEmpty()) {
            return CommonResult.failed(ResultCode.EMAIL_NOT_FOUND);
        }
        // 生成 6 位随机验证码
        String code = RandomUtil.randomNumbers(6);
        // 存入 Redis，过期 5 分钟
        String redisKey = "verify:code:" + email.trim();
        redisService.set(redisKey, code, 5 * 60);
        LOGGER.info("密码重置验证码已生成，邮箱: {}", maskEmail(email));
        return CommonResult.success(buildVerificationResult(code));
    }

    @ApiOperation(value = "忘记密码-重置密码")
    @RequestMapping(value = "/forgot/reset", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult resetPassword(@RequestBody Map<String, String> param) {
        String email = param.get("email");
        String code = param.get("code");
        String newPassword = param.get("newPassword");
        String confirmPassword = param.get("confirmPassword");
        if (email == null || email.trim().isEmpty()) {
            return CommonResult.validateFailed("邮箱不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return CommonResult.validateFailed("验证码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return CommonResult.validateFailed("新密码不能为空");
        }
        if (!newPassword.equals(confirmPassword)) {
            return CommonResult.failed(ResultCode.PASSWORD_CONFIRM_MISMATCH);
        }
        // 校验验证码
        String redisKey = "verify:code:" + email.trim();
        String storedCode = (String) redisService.get(redisKey);
        if (storedCode == null || !storedCode.equals(code.trim())) {
            return CommonResult.validateFailed("验证码错误或已过期");
        }
        // 查找用户
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdmin::getEmail, email.trim());
        List<UmsAdmin> adminList = adminService.list(wrapper);
        if (adminList == null || adminList.isEmpty()) {
            return CommonResult.failed(ResultCode.EMAIL_NOT_FOUND);
        }
        // 更新密码
        UmsAdmin admin = adminList.get(0);
        admin.setPassword(passwordEncoder.encode(newPassword));
        adminService.updateById(admin);
        // 删除验证码
        redisService.del(redisKey);
        return CommonResult.success(null, "密码重置成功");
    }

    @ApiOperation(value = "登录以后返回token")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult login(@Validated @RequestBody UmsAdminLoginParam umsAdminLoginParam) {
        // 校验图片验证码
        if (!validateCaptcha(umsAdminLoginParam.getCaptchaUuid(), umsAdminLoginParam.getCaptchaCode())) {
            return CommonResult.validateFailed("图片验证码错误或已过期");
        }
        String token = adminService.login(umsAdminLoginParam.getUsername(), umsAdminLoginParam.getPassword());
        if (token == null) {
            return CommonResult.validateFailed("用户名或密码错误");
        }
        UmsAdmin umsAdmin = adminService.getAdminByUsername(umsAdminLoginParam.getUsername());
        org.springframework.security.core.userdetails.UserDetails userDetails =
                adminService.loadUserByUsername(umsAdminLoginParam.getUsername());
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("token", token);
        tokenMap.put("tokenHead", tokenHead);
        tokenMap.put("refreshToken", jwtTokenUtil.generateRefreshToken(userDetails));
        if (umsAdmin != null) {
            List<UmsRole> roleList = adminService.getRoleList(umsAdmin.getId());
            if (CollUtil.isNotEmpty(roleList)) {
                List<String> roles = roleList.stream().map(UmsRole::getName).collect(Collectors.toList());
                tokenMap.put("roles", roles);
            }
            // 查询用户资源权限URL列表
            List<UmsResource> resourceList = adminService.getResourceList(umsAdmin.getId());
            if (CollUtil.isNotEmpty(resourceList)) {
                List<String> resources = resourceList.stream()
                        .map(UmsResource::getUrl)
                        .filter(url -> url != null && !url.isEmpty())
                        .collect(Collectors.toList());
                tokenMap.put("resources", resources);
            }
        }
        return CommonResult.success(tokenMap);
    }

    @ApiOperation(value = "邮箱验证码登录")
    @RequestMapping(value = "/login-by-code", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult loginByCode(@RequestBody Map<String, String> param) {
        String email = param.get("email");
        String code = param.get("code");
        if (email == null || email.trim().isEmpty()) {
            return CommonResult.validateFailed("邮箱不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return CommonResult.validateFailed("验证码不能为空");
        }
        // 校验验证码
        String redisKey = "verify:login:" + email.trim();
        String storedCode = (String) redisService.get(redisKey);
        if (storedCode == null || !storedCode.equals(code.trim())) {
            return CommonResult.validateFailed("验证码错误或已过期");
        }
        redisService.del(redisKey);
        // 根据邮箱查找用户
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdmin::getEmail, email.trim());
        List<UmsAdmin> adminList = adminService.list(wrapper);
        if (adminList == null || adminList.isEmpty()) {
            return CommonResult.failed(ResultCode.EMAIL_NOT_FOUND);
        }
        UmsAdmin umsAdmin = adminList.get(0);
        // 检查用户状态
        if (umsAdmin.getStatus() != 1) {
            return CommonResult.failed("帐号已被禁用");
        }
        // 加载用户详情并生成 token
        org.springframework.security.core.userdetails.UserDetails userDetails =
                adminService.loadUserByUsername(umsAdmin.getUsername());
        com.m998.civilservice.security.util.JwtTokenUtil jwtTokenUtil =
                com.m998.civilservice.security.util.SpringUtil.getBean(com.m998.civilservice.security.util.JwtTokenUtil.class);
        String token = jwtTokenUtil.generateAccessToken(userDetails);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);
        if (token == null) {
            return CommonResult.failed("登录失败，请检查用户状态");
        }
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("token", token);
        tokenMap.put("tokenHead", tokenHead);
        tokenMap.put("refreshToken", refreshToken);
        List<UmsRole> roleList = adminService.getRoleList(umsAdmin.getId());
        if (CollUtil.isNotEmpty(roleList)) {
            List<String> roles = roleList.stream().map(UmsRole::getName).collect(Collectors.toList());
            tokenMap.put("roles", roles);
        }
        List<UmsResource> resourceList = adminService.getResourceList(umsAdmin.getId());
        if (CollUtil.isNotEmpty(resourceList)) {
            List<String> resources = resourceList.stream()
                    .map(UmsResource::getUrl)
                    .filter(url -> url != null && !url.isEmpty())
                    .collect(Collectors.toList());
            tokenMap.put("resources", resources);
        }
        return CommonResult.success(tokenMap);
    }

    @ApiOperation(value = "发送登录邮箱验证码")
    @RequestMapping(value = "/send-login-code", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult sendLoginCode(@RequestBody Map<String, String> param) {
        String email = param.get("email");
        String captchaUuid = param.get("captchaUuid");
        String captchaCode = param.get("captchaCode");
        if (email == null || email.trim().isEmpty()) {
            return CommonResult.validateFailed("邮箱不能为空");
        }
        // 校验图片验证码
        if (!validateCaptcha(captchaUuid, captchaCode)) {
            return CommonResult.validateFailed("图片验证码错误或已过期");
        }
        // 检查邮箱是否绑定用户
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdmin::getEmail, email.trim());
        List<UmsAdmin> adminList = adminService.list(wrapper);
        if (adminList == null || adminList.isEmpty()) {
            return CommonResult.failed(ResultCode.EMAIL_NOT_FOUND);
        }
        // 生成 6 位随机验证码
        String code = RandomUtil.randomNumbers(6);
        String redisKey = "verify:login:" + email.trim();
        redisService.set(redisKey, code, 5 * 60);
        LOGGER.info("登录验证码已生成，邮箱: {}", maskEmail(email));
        return CommonResult.success(buildVerificationResult(code), "验证码已发送");
    }

    private Map<String, String> buildVerificationResult(String code) {
        Map<String, String> result = new HashMap<>();
        if (exposeVerificationCode) {
            result.put("code", code);
        }
        return result;
    }

    private String maskEmail(String email) {
        int separator = email.indexOf('@');
        if (separator <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(separator);
    }

    @ApiOperation(value = "刷新token")
    @RequestMapping(value = "/refreshToken", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult refreshToken(HttpServletRequest request) {
        String token = request.getHeader(tokenHeader);
        String refreshToken = adminService.refreshToken(token);
        if (refreshToken == null) {
            return CommonResult.failed("token已经过期！");
        }
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", refreshToken);
        tokenMap.put("tokenHead", tokenHead);
        return CommonResult.success(tokenMap);
    }

    @ApiOperation(value = "获取当前登录用户信息")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult getAdminInfo(Principal principal) {
        if (principal == null) {
            return CommonResult.unauthorized(null);
        }
        String username = principal.getName();
        UmsAdmin umsAdmin = adminService.getAdminByUsername(username);
        if (umsAdmin == null) {
            return CommonResult.unauthorized(null);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("username", umsAdmin.getUsername());
        data.put("icon", umsAdmin.getIcon());
        try {
            List<?> menuList = roleService.getMenuList(umsAdmin.getId());
            data.put("menus", menuList != null ? menuList : new ArrayList<>());
            List<UmsRole> roleList = adminService.getRoleList(umsAdmin.getId());
            if (CollUtil.isNotEmpty(roleList)) {
                List<String> roles = roleList.stream().map(UmsRole::getName).collect(Collectors.toList());
                data.put("roles", roles);
            }
        } catch (Exception e) {
            data.put("menus", new ArrayList<>());
        }
        List<UmsResource> resourceList = adminService.getResourceList(umsAdmin.getId());
        if (CollUtil.isNotEmpty(resourceList)) {
            List<String> resources = resourceList.stream()
                    .map(UmsResource::getUrl)
                    .filter(url -> url != null && !url.isEmpty())
                    .collect(Collectors.toList());
            data.put("resources", resources);
        }
        return CommonResult.success(data);
    }

    @ApiOperation(value = "登出功能")
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult logout(@RequestBody(required = false) Map<String, String> param,
                                HttpServletRequest request) {
        String authHeader = request.getHeader(tokenHeader);
        if (authHeader != null && authHeader.startsWith(tokenHead)) {
            String token = authHeader.substring(tokenHead.length());
            String jti = jwtTokenUtil.getJti(token);
            if (jti != null) {
                redisService.set("blacklist:access:" + jti, "1", jwtTokenUtil.getRemainingExpiration(token));
            }
        }
        if (param != null && param.containsKey("refreshToken")) {
            String rt = param.get("refreshToken");
            String rtJti = jwtTokenUtil.getJti(rt);
            if (rtJti != null) {
                redisService.set("blacklist:refresh:" + rtJti, "1", jwtTokenUtil.getRemainingExpiration(rt));
            }
        }
        return CommonResult.success(null);
    }

    @ApiOperation(value = "刷新Access Token")
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult refresh(@RequestBody Map<String, String> param) {
        String refreshToken = param.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return CommonResult.validateFailed("refreshToken不能为空");
        }
        if (!jwtTokenUtil.isRefreshToken(refreshToken)) {
            return CommonResult.validateFailed("非法的refreshToken");
        }
        if (jwtTokenUtil.isTokenExpired(refreshToken)) {
            return CommonResult.validateFailed("refreshToken已过期，请重新登录");
        }
        String jti = jwtTokenUtil.getJti(refreshToken);
        if (jti != null && redisService.hasKey("blacklist:refresh:" + jti)) {
            return CommonResult.validateFailed("refreshToken已被使用，请重新登录");
        }
        // 旧 Refresh Token 加入黑名单
        redisService.set("blacklist:refresh:" + jti, "1", jwtTokenUtil.getRemainingExpiration(refreshToken));
        // 加载用户信息并生成新 Token
        String username = jwtTokenUtil.getUserNameFromToken(refreshToken);
        org.springframework.security.core.userdetails.UserDetails userDetails =
                adminService.loadUserByUsername(username);
        String newAccessToken = jwtTokenUtil.generateAccessToken(userDetails);
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);
        Map<String, Object> result = new HashMap<>();
        result.put("token", newAccessToken);
        result.put("tokenHead", tokenHead);
        result.put("refreshToken", newRefreshToken);
        return CommonResult.success(result);
    }


    @ApiOperation("根据用户名或姓名分页获取用户列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<UmsAdmin>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
                                                   @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        Page<UmsAdmin> adminList = adminService.list(keyword, pageSize, pageNum);
        return CommonResult.success(CommonPage.restPage(adminList));
    }

    @ApiOperation("获取指定用户信息")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<UmsAdmin> getItem(@PathVariable Long id) {
        UmsAdmin admin = adminService.getById(id);
        return CommonResult.success(admin);
    }

    @ApiOperation("修改指定用户信息")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody UmsAdmin admin) {
        boolean success = adminService.update(id, admin);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation("修改指定用户密码")
    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updatePassword(@Validated @RequestBody UpdateAdminPasswordParam updatePasswordParam) {
        int status = adminService.updatePassword(updatePasswordParam);
        if (status > 0) {
            return CommonResult.success(status);
        } else if (status == -1) {
            return CommonResult.failed("提交参数不合法");
        } else if (status == -2) {
            return CommonResult.failed("找不到该用户");
        } else if (status == -3) {
            return CommonResult.failed("旧密码错误");
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("删除指定用户信息")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        boolean success = adminService.delete(id);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation("修改帐号状态")
    @RequestMapping(value = "/updateStatus/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateStatus(@PathVariable Long id, @RequestParam(value = "status") Integer status) {
        UmsAdmin umsAdmin = new UmsAdmin();
        umsAdmin.setStatus(status);
        boolean success = adminService.update(id, umsAdmin);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation("给用户分配角色")
    @RequestMapping(value = "/role/update", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateRole(@RequestParam("adminId") Long adminId,
                                   @RequestParam("roleIds") List<Long> roleIds) {
        int count = adminService.updateRole(adminId, roleIds);
        if (count >= 0) {
            return CommonResult.success(count);
        }
        return CommonResult.failed();
    }

    @ApiOperation("获取指定用户的角色")
    @RequestMapping(value = "/role/{adminId}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<UmsRole>> getRoleList(@PathVariable Long adminId) {
        List<UmsRole> roleList = adminService.getRoleList(adminId);
        return CommonResult.success(roleList);
    }
}
