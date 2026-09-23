package com.m998.civilservice.modules.favorite.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.m998.civilservice.modules.favorite.dto.FavoriteDto;
import com.m998.civilservice.modules.favorite.model.Favorite;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 收藏表 服务类
 * </p>
 *
 * @since 2026-04-10
 */
public interface FavoriteService extends IService<Favorite> {

    /**
     * 添加收藏
     * @param positionId 岗位ID
     * @return 是否成功
     */
    boolean addFavorite(Long positionId);

    /**
     * 取消收藏
     * @param positionId 岗位ID
     * @return 是否成功
     */
    boolean removeFavorite(Long positionId);

    /**
     * 获取当前用户的收藏列表
     * @return 收藏DTO列表
     */
    List<FavoriteDto> getMyFavorites();

    /**
     * 分页获取当前用户的收藏列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<FavoriteDto> getMyFavoritesPage(Integer pageNum, Integer pageSize);

    /**
     * 检查是否已收藏
     * @param positionId 岗位ID
     * @return 是否已收藏
     */
    boolean isFavorite(Long positionId);
}
