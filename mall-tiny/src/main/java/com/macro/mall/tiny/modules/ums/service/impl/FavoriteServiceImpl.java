package com.macro.mall.tiny.modules.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.macro.mall.tiny.domain.AdminUserDetails;
import com.macro.mall.tiny.modules.ums.dto.FavoriteDto;
import com.macro.mall.tiny.modules.ums.mapper.FavoriteMapper;
import com.macro.mall.tiny.modules.ums.model.Favorite;
import com.macro.mall.tiny.modules.ums.service.FavoriteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 收藏表 服务实现类
 * </p>
 *
 * @author macro
 * @since 2026-04-10
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Override
    public boolean addFavorite(Long positionId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // 检查是否已收藏
        if (isFavorite(positionId)) {
            return true; // 已收藏，直接返回成功
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setPositionId(positionId);
        favorite.setCreateTime(new Date());
        favorite.setUpdateTime(new Date());
        return save(favorite);
    }

    @Override
    public boolean removeFavorite(Long positionId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getPositionId, positionId);
        return remove(wrapper);
    }

    @Override
    public List<FavoriteDto> getMyFavorites() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return baseMapper.selectFavoriteListByUserId(userId);
    }

    @Override
    public Page<FavoriteDto> getMyFavoritesPage(Integer pageNum, Integer pageSize) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        Page<FavoriteDto> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectFavoritePageByUserId(page, userId);
    }

    @Override
    public boolean isFavorite(Long positionId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getPositionId, positionId);
        return count(wrapper) > 0;
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserDetails) {
            return ((AdminUserDetails) principal).getUmsAdmin().getId();
        }
        return null;
    }
}
