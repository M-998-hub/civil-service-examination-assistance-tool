package com.m998.civilservice.modules.favorite.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.m998.civilservice.domain.AdminUserDetails;
import com.m998.civilservice.modules.favorite.dto.FavoriteDto;
import com.m998.civilservice.modules.favorite.mapper.FavoriteMapper;
import com.m998.civilservice.modules.favorite.model.Favorite;
import com.m998.civilservice.modules.favorite.service.FavoriteService;
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
 * @since 2026-04-10
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    /**
     * 添加收藏
     * @param positionId 岗位ID
     * @return
     */
    @Override
    public boolean addFavorite(Long positionId) {
        // 获取当前登录用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false; // 用户未登录，添加失败
        }

        // 检查是否已收藏
        if (isFavorite(positionId)) {
            return true; // 已收藏，直接返回成功
        }

        // 构建收藏实体
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);  // 收藏人
        favorite.setPositionId(positionId); // 收藏的岗位
        favorite.setCreateTime(new Date()); // 创建时间
        favorite.setUpdateTime(new Date()); // 更新时间
        return save(favorite); // 调用 MyBatis-Plus 的 save 方法插入数据库
    }

    /**
     * 取消收藏
     * @param positionId 岗位ID
     * @return
     */
    @Override
    public boolean removeFavorite(Long positionId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // 构建查询条件 （指定用户和岗位）
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getPositionId, positionId);

        // 删除匹配的记录
        return remove(wrapper);
    }

    /**
     * 获取当前用户的收藏列表
     * @return
     */
    @Override
    public List<FavoriteDto> getMyFavorites() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }

        // 查询该用户的所有收藏（关联岗位信息）
        return baseMapper.selectFavoriteListByUserId(userId);
    }

    @Override
    public Page<FavoriteDto> getMyFavoritesPage(Integer pageNum, Integer pageSize) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }

        // 创建分页对象
        Page<FavoriteDto> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectFavoritePageByUserId(page, userId);
    }

    /**
     * 检查用户是否已收藏某岗位
     * @param positionId 岗位ID
     * @return
     */
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
        // 从 SecurityContextHolder 获取 Spring Security 的认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 检查认证对象是否存在且有效
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // 获取认证主体（用户信息）
        Object principal = authentication.getPrincipal();

        // 从认证信息中提取 AdminUserDetails
        if (principal instanceof AdminUserDetails) {
            return ((AdminUserDetails) principal).getUmsAdmin().getId();
        }

        // 其他情况（如匿名用户、非用户对象）返回null
        return null;
    }
}
