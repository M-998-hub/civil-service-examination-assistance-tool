package com.m998.civilservice.modules.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.m998.civilservice.modules.profile.model.UserArchive;
import com.m998.civilservice.modules.profile.mapper.UserArchiveMapper;
import com.m998.civilservice.modules.profile.service.UserArchiveService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * <p>
 * 用户档案表 服务实现类
 * </p>
 *
 * @since 2026-04-10
 */
@Service
public class UserArchiveServiceImpl extends ServiceImpl<UserArchiveMapper, UserArchive> implements UserArchiveService {

    @Override
    public UserArchive getByUserId(Long userId) {
        LambdaQueryWrapper<UserArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserArchive::getUserId, userId);
        return getOne(wrapper);
    }

    @Override
    public UserArchive getMyArchive() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return getByUserId(userId);
    }

    @Override
    public boolean saveMyArchive(UserArchive userArchive) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }
        
        // 查询是否已有档案
        UserArchive existing = getByUserId(userId);
        if (existing != null) {
            // 更新
            userArchive.setId(existing.getId());
            userArchive.setUserId(userId);
            userArchive.setUpdateTime(new Date());
            return updateById(userArchive);
        } else {
            // 新增
            userArchive.setUserId(userId);
            userArchive.setCreateTime(new Date());
            userArchive.setUpdateTime(new Date());
            return save(userArchive);
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // 从 authentication 中获取用户ID
        // 这里需要根据实际的用户信息存储方式调整
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.m998.civilservice.domain.AdminUserDetails) {
            return ((com.m998.civilservice.domain.AdminUserDetails) principal).getUmsAdmin().getId();
        }
        return null;
    }
}
