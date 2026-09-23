package com.m998.civilservice.modules.profile.service;

import com.m998.civilservice.modules.profile.model.UserArchive;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户档案表 服务类
 * </p>
 *
 * @since 2026-04-10
 */
public interface UserArchiveService extends IService<UserArchive> {

    /**
     * 根据用户ID获取档案
     */
    UserArchive getByUserId(Long userId);

    /**
     * 获取当前用户档案
     */
    UserArchive getMyArchive();

    /**
     * 保存当前用户档案
     */
    boolean saveMyArchive(UserArchive userArchive);
}
