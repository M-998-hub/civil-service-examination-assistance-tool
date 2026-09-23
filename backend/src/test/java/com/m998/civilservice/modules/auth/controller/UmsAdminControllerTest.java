package com.m998.civilservice.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.m998.civilservice.common.service.RedisService;
import com.m998.civilservice.modules.auth.dto.UmsAdminLoginParam;
import com.m998.civilservice.modules.auth.model.UmsAdmin;
import com.m998.civilservice.modules.auth.service.UmsAdminService;
import com.m998.civilservice.modules.auth.service.UmsRoleService;
import com.m998.civilservice.security.util.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UmsAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("用户认证接口测试")
class UmsAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private UmsAdminService adminService;
    @MockBean
    private UmsRoleService roleService;
    @MockBean
    private RedisService redisService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @Test
    @DisplayName("验证码和账号正确时返回访问令牌")
    void login_valid_returnsTokens() throws Exception {
        when(redisService.get("captcha:captcha-id")).thenReturn("ABCD");
        when(adminService.login("admin", "123456")).thenReturn("access-token");
        UmsAdmin admin = new UmsAdmin();
        admin.setId(1L);
        admin.setUsername("admin");
        when(adminService.getAdminByUsername("admin")).thenReturn(admin);
        when(adminService.loadUserByUsername("admin")).thenReturn(mock(UserDetails.class));
        when(adminService.getRoleList(1L)).thenReturn(Collections.emptyList());
        when(adminService.getResourceList(1L)).thenReturn(Collections.emptyList());
        when(jwtTokenUtil.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");

        UmsAdminLoginParam param = loginParam("admin", "123456", "captcha-id", "abcd");
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("验证码错误时拒绝登录")
    void login_invalidCaptcha_returnsValidationError() throws Exception {
        when(redisService.get("captcha:captcha-id")).thenReturn("ABCD");
        UmsAdminLoginParam param = loginParam("admin", "123456", "captcha-id", "wrong");

        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("图片验证码错误或已过期"));
    }

    @Test
    @DisplayName("空密码触发参数校验")
    void login_emptyPassword_returnsValidationError() throws Exception {
        UmsAdminLoginParam param = loginParam("admin", "", "captcha-id", "ABCD");
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("错误账号密码返回校验失败")
    void login_wrongCredentials_returnsValidationError() throws Exception {
        when(redisService.get("captcha:captcha-id")).thenReturn("ABCD");
        when(adminService.login(anyString(), anyString())).thenReturn(null);
        UmsAdminLoginParam param = loginParam("admin", "wrong", "captcha-id", "ABCD");

        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("注册流程校验图片验证码和邮箱验证码")
    void register_validVerificationCodes_returnsSuccess() throws Exception {
        when(redisService.get("captcha:captcha-id")).thenReturn("ABCD");
        when(redisService.get("register:code:user@example.com")).thenReturn("123456");
        when(adminService.getAdminByUsername("newuser")).thenReturn(null);
        when(adminService.register(any())).thenReturn(new UmsAdmin());

        Map<String, String> body = new HashMap<>();
        body.put("username", "newuser");
        body.put("password", "password");
        body.put("confirmPassword", "password");
        body.put("email", "user@example.com");
        body.put("emailCode", "123456");
        body.put("captchaUuid", "captcha-id");
        body.put("captchaCode", "ABCD");

        mockMvc.perform(post("/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private UmsAdminLoginParam loginParam(
            String username, String password, String captchaUuid, String captchaCode) {
        UmsAdminLoginParam param = new UmsAdminLoginParam();
        param.setUsername(username);
        param.setPassword(password);
        param.setCaptchaUuid(captchaUuid);
        param.setCaptchaCode(captchaCode);
        return param;
    }
}
