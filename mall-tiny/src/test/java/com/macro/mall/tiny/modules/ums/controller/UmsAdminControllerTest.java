package com.macro.mall.tiny.modules.ums.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.tiny.common.exception.GlobalExceptionHandler;
import com.macro.mall.tiny.modules.ums.dto.UmsAdminLoginParam;
import com.macro.mall.tiny.modules.ums.dto.UmsAdminParam;
import com.macro.mall.tiny.modules.ums.dto.UpdateAdminPasswordParam;
import com.macro.mall.tiny.modules.ums.model.UmsAdmin;
import com.macro.mall.tiny.modules.ums.model.UmsRole;
import com.macro.mall.tiny.modules.ums.service.UmsAdminService;
import com.macro.mall.tiny.modules.ums.service.UmsRoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理员Controller集成测试
 */
@WebMvcTest(UmsAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UmsAdminController 集成测试")
class UmsAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UmsAdminService adminService;

    @MockBean
    private UmsRoleService roleService;

    @Test
    @DisplayName("登录成功 - 返回200和token")
    void login_valid_returns200() throws Exception {
        when(adminService.login(anyString(), anyString())).thenReturn("test-jwt-token");
        UmsAdmin admin = new UmsAdmin();
        admin.setId(1L);
        admin.setUsername("admin");
        when(adminService.getAdminByUsername("admin")).thenReturn(admin);
        when(adminService.getRoleList(1L)).thenReturn(Collections.emptyList());
        when(adminService.getResourceList(1L)).thenReturn(Collections.emptyList());

        UmsAdminLoginParam loginParam = new UmsAdminLoginParam();
        loginParam.setUsername("admin");
        loginParam.setPassword("123456");

        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginParam)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("test-jwt-token"));
    }

    @Test
    @DisplayName("登录失败(空密码) - 返回400参数校验失败")
    void login_emptyPassword_returns400() throws Exception {
        String json = "{\"username\":\"admin\",\"password\":\"\"}";

        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("登录失败(畸形JSON) - 返回400")
    void login_invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("登录失败(token为null) - 返回校验失败")
    void login_wrongCredentials_returnsFailed() throws Exception {
        when(adminService.login(anyString(), anyString())).thenReturn(null);

        UmsAdminLoginParam loginParam = new UmsAdminLoginParam();
        loginParam.setUsername("admin");
        loginParam.setPassword("wrong");

        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginParam)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("注册成功 - 返回200")
    void register_success_returns200() throws Exception {
        UmsAdmin savedAdmin = new UmsAdmin();
        savedAdmin.setId(1L);
        savedAdmin.setUsername("newuser");
        when(adminService.register(any(UmsAdminParam.class))).thenReturn(savedAdmin);

        UmsAdminParam param = new UmsAdminParam();
        param.setUsername("newuser");
        param.setPassword("123456");

        mockMvc.perform(post("/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    @DisplayName("注册失败(重复用户名) - 返回500操作失败")
    void register_duplicate_returnsFailed() throws Exception {
        when(adminService.register(any(UmsAdminParam.class))).thenReturn(null);

        UmsAdminParam param = new UmsAdminParam();
        param.setUsername("existinguser");
        param.setPassword("123456");

        mockMvc.perform(post("/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("修改密码成功 - 返回200")
    void updatePassword_valid_returns200() throws Exception {
        when(adminService.updatePassword(any(UpdateAdminPasswordParam.class))).thenReturn(1);

        UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
        param.setUsername("admin");
        param.setOldPassword("oldpass");
        param.setNewPassword("newpass");

        mockMvc.perform(post("/admin/updatePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("修改密码失败(旧密码错误) - 返回500")
    void updatePassword_wrongOldPwd_returnsFailed() throws Exception {
        when(adminService.updatePassword(any(UpdateAdminPasswordParam.class))).thenReturn(-3);

        UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
        param.setUsername("admin");
        param.setOldPassword("wrong");
        param.setNewPassword("newpass");

        mockMvc.perform(post("/admin/updatePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("旧密码错误"));
    }
}
