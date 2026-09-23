package com.m998.civilservice.modules.favorite.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.m998.civilservice.modules.favorite.dto.FavoriteDto;
import com.m998.civilservice.modules.favorite.model.Favorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 收藏表 Mapper 接口
 * </p>
 *
 * @since 2026-04-10
 */
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 获取用户的收藏列表（关联岗位信息）
     * @param userId 用户ID
     * @return 收藏DTO列表
     */
    List<FavoriteDto> selectFavoriteListByUserId(@Param("userId") Long userId);

    /**
     * 分页获取用户的收藏列表（关联岗位信息）
     * @param page 分页对象
     * @param userId 用户ID
     * @return 分页结果
     */
    Page<FavoriteDto> selectFavoritePageByUserId(Page<FavoriteDto> page, @Param("userId") Long userId);
}
