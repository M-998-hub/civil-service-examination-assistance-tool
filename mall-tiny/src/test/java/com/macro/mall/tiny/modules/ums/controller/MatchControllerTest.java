package com.macro.mall.tiny.modules.ums.controller;

import com.macro.mall.tiny.common.exception.ApiException;
import com.macro.mall.tiny.common.api.ResultCode;
import com.macro.mall.tiny.modules.ums.dto.MatchResultDto;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.model.UmsAdmin;
import com.macro.mall.tiny.modules.ums.model.UserArchive;
import com.macro.mall.tiny.modules.ums.service.MatchService;
import com.macro.mall.tiny.modules.ums.service.UmsAdminService;
import com.macro.mall.tiny.modules.ums.service.UserArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 匹配Controller集成测试
 */
@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MatchController 集成测试")
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private UmsAdminService adminService;

    @MockBean
    private UserArchiveService userArchiveService;

    @Test
    @DisplayName("匹配成功 - 返回200和匹配列表")
    void recommend_success_returnsMatchList() throws Exception {
        UmsAdmin admin = new UmsAdmin();
        admin.setId(1L);
        admin.setUsername("testuser");
        when(adminService.getAdminByUsername("testuser")).thenReturn(admin);

        UserArchive archive = new UserArchive();
        archive.setMajor("软件工程");
        archive.setEducation("本科");
        when(userArchiveService.getByUserId(1L)).thenReturn(archive);

        Position position = new Position();
        position.setPositionName("开发岗");
        MatchResultDto dto = new MatchResultDto(position, 35, Arrays.asList("专业完全匹配+30分", "学历刚好满足+5分"));
        when(matchService.recommend(any(UserArchive.class))).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(post("/match/recommend")
                        .principal(() -> "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].matchScore").value(35));
    }

    @Test
    @DisplayName("无用户档案 - 返回失败")
    void recommend_noArchive_returnsFailed() throws Exception {
        UmsAdmin admin = new UmsAdmin();
        admin.setId(1L);
        admin.setUsername("testuser");
        when(adminService.getAdminByUsername("testuser")).thenReturn(admin);
        when(userArchiveService.getByUserId(1L)).thenReturn(null);

        mockMvc.perform(post("/match/recommend")
                        .principal(() -> "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请先完善个人档案"));
    }

    @Test
    @DisplayName("未认证用户 - 返回401")
    void recommend_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/match/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
