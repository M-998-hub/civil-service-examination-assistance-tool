package com.macro.mall.tiny.modules.ums.service;

import com.macro.mall.tiny.common.exception.ApiException;
import com.macro.mall.tiny.domain.AdminUserDetails;
import com.macro.mall.tiny.modules.ums.dto.UmsAdminParam;
import com.macro.mall.tiny.modules.ums.dto.UpdateAdminPasswordParam;
import com.macro.mall.tiny.modules.ums.mapper.UmsAdminLoginLogMapper;
import com.macro.mall.tiny.modules.ums.mapper.UmsAdminMapper;
import com.macro.mall.tiny.modules.ums.mapper.UmsResourceMapper;
import com.macro.mall.tiny.modules.ums.mapper.UmsRoleMapper;
import com.macro.mall.tiny.modules.ums.model.UmsAdmin;
import com.macro.mall.tiny.modules.ums.model.UmsAdminRoleRelation;
import com.macro.mall.tiny.modules.ums.model.UmsResource;
import com.macro.mall.tiny.modules.ums.service.impl.UmsAdminServiceImpl;
import com.macro.mall.tiny.security.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 管理员服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UmsAdminService 单元测试")
class UmsAdminServiceTest {

    @Mock
    private UmsAdminMapper adminMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UmsAdminLoginLogMapper loginLogMapper;

    @Mock
    private UmsAdminRoleRelationService adminRoleRelationService;

    @Mock
    private UmsRoleMapper roleMapper;

    @Mock
    private UmsResourceMapper resourceMapper;

    @Mock
    private UmsAdminCacheService cacheService;

    @Spy
    @InjectMocks
    private UmsAdminServiceImpl adminService;

    // === 密码修改测试 ===

    @Nested
    @DisplayName("密码修改 updatePassword")
    class UpdatePasswordTests {

        @Test
        @DisplayName("参数为空 - 返回-1")
        void updatePassword_emptyParams_returnsNeg1() {
            UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
            param.setUsername("");
            param.setOldPassword("old");
            param.setNewPassword("new");

            int result = adminService.updatePassword(param);
            assertThat(result).isEqualTo(-1);
        }

        @Test
        @DisplayName("旧密码为空 - 返回-1")
        void updatePassword_emptyOldPassword_returnsNeg1() {
            UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
            param.setUsername("admin");
            param.setOldPassword("");
            param.setNewPassword("new");

            int result = adminService.updatePassword(param);
            assertThat(result).isEqualTo(-1);
        }

        @Test
        @DisplayName("新密码为空 - 返回-1")
        void updatePassword_emptyNewPassword_returnsNeg1() {
            UpdateAdminPasswordParam param = new UpdateAdminPasswordParam();
            param.setUsername("admin");
            param.setOldPassword("old");
            param.setNewPassword("");

            int result = adminService.updatePassword(param);
            assertThat(result).isEqualTo(-1);
        }
    }

    // === 角色更新测试 ===

    @Nested
    @DisplayName("角色更新 updateRole")
    class UpdateRoleTests {

        @Test
        @DisplayName("分配角色 - 先删后增")
        void updateRole_withRoles_deletesAndInserts() {
            List<Long> roleIds = Arrays.asList(1L, 2L);
            when(adminRoleRelationService.remove(any())).thenReturn(true);
            when(adminRoleRelationService.saveBatch(any())).thenReturn(true);
            doReturn(cacheService).when(adminService).getCacheService();

            int count = adminService.updateRole(1L, roleIds);

            assertThat(count).isEqualTo(2);
            verify(adminRoleRelationService).remove(any());
            verify(adminRoleRelationService).saveBatch(any());
        }

        @Test
        @DisplayName("空角色列表 - 仅删除")
        void updateRole_emptyRoles_onlyDeletes() {
            List<Long> roleIds = Collections.emptyList();
            when(adminRoleRelationService.remove(any())).thenReturn(true);
            doReturn(cacheService).when(adminService).getCacheService();

            int count = adminService.updateRole(1L, roleIds);

            assertThat(count).isEqualTo(0);
            verify(adminRoleRelationService).remove(any());
            verify(adminRoleRelationService, never()).saveBatch(any());
        }

        @Test
        @DisplayName("null角色列表 - 仅删除")
        void updateRole_nullRoles_onlyDeletes() {
            when(adminRoleRelationService.remove(any())).thenReturn(true);
            doReturn(cacheService).when(adminService).getCacheService();

            int count = adminService.updateRole(1L, null);

            assertThat(count).isEqualTo(0);
            verify(adminRoleRelationService).remove(any());
            verify(adminRoleRelationService, never()).saveBatch(any());
        }
    }
}
