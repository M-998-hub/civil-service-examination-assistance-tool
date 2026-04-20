package com.macro.mall.tiny.modules.ums.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.macro.mall.tiny.common.api.CommonPage;
import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.modules.ums.dto.FavoriteDto;
import com.macro.mall.tiny.modules.ums.model.Favorite;
import com.macro.mall.tiny.modules.ums.service.FavoriteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏表 前端控制器
 * Created by macro on 2026-04-10.
 */
@Controller
@Api(tags = "FavoriteController")
@Tag(name = "FavoriteController", description = "收藏表")
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @ApiOperation(value = "添加Favorite")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestBody Favorite favorite) {
        boolean success = favoriteService.save(favorite);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "修改Favorite")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody Favorite favorite) {
        favorite.setId(id);
        boolean success = favoriteService.updateById(favorite);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "删除Favorite")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        boolean success = favoriteService.removeById(id);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed();
    }

    @ApiOperation(value = "根据ID获取Favorite")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<Favorite> getItem(@PathVariable Long id) {
        Favorite favorite = favoriteService.getById(id);
        return CommonResult.success(favorite);
    }

    @ApiOperation(value = "分页获取Favorite列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<Favorite>> list(
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        Page<Favorite> page = favoriteService.page(new Page<>(pageNum, pageSize));
        return CommonResult.success(CommonPage.restPage(page));
    }

    @ApiOperation(value = "添加收藏")
    @RequestMapping(value = "/add/{positionId}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult addFavorite(@PathVariable Long positionId) {
        boolean success = favoriteService.addFavorite(positionId);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed("收藏失败");
    }

    @ApiOperation(value = "取消收藏")
    @RequestMapping(value = "/remove/{positionId}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult removeFavorite(@PathVariable Long positionId) {
        boolean success = favoriteService.removeFavorite(positionId);
        if (success) {
            return CommonResult.success(null);
        }
        return CommonResult.failed("取消收藏失败");
    }

    @ApiOperation(value = "获取我的收藏列表")
    @RequestMapping(value = "/my", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<FavoriteDto>> getMyFavorites() {
        List<FavoriteDto> list = favoriteService.getMyFavorites();
        return CommonResult.success(list);
    }

    @ApiOperation(value = "分页获取我的收藏列表")
    @RequestMapping(value = "/my/page", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<FavoriteDto>> getMyFavoritesPage(
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        Page<FavoriteDto> page = favoriteService.getMyFavoritesPage(pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(page));
    }

    @ApiOperation(value = "检查是否已收藏")
    @RequestMapping(value = "/isFavorite/{positionId}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<Boolean> isFavorite(@PathVariable Long positionId) {
        boolean isFavorite = favoriteService.isFavorite(positionId);
        return CommonResult.success(isFavorite);
    }
}
