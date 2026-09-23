package com.m998.civilservice.modules.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.m998.civilservice.common.service.RedisService;
import com.m998.civilservice.modules.auth.mapper.UmsAdminMapper;
import com.m998.civilservice.modules.auth.model.UmsAdmin;
import com.m998.civilservice.modules.auth.model.UmsAdminRoleRelation;
import com.m998.civilservice.modules.auth.model.UmsResource;
import com.m998.civilservice.modules.auth.service.UmsAdminCacheService;
import com.m998.civilservice.modules.auth.service.UmsAdminRoleRelationService;
import com.m998.civilservice.modules.auth.service.UmsAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台用户缓存管理Service实现类
 *
 * 功能说明：管理用户信息和权限资源的 Redis 缓存
 */
@Service
public class UmsAdminCacheServiceImpl implements UmsAdminCacheService {

    // 用户服务（用于查询数据库中的用户信息）
    @Autowired
    private UmsAdminService adminService;

    // Redis 操作服务（提供 get/set/del等基础操作）
    @Autowired
    private RedisService redisService;

    // 用户 Mapper（用于自定义 SQL 查询）
    @Autowired
    private UmsAdminMapper adminMapper;

    // 用户-角色关联服务
    @Autowired
    private UmsAdminRoleRelationService adminRoleRelationService;

    // Redis 配置参数（从配置文件注入）
    @Value("${redis.database}")
    private String REDIS_DATABASE; // Redis 数据库编号（用于区分不同业务的数据）
    @Value("${redis.expire.common}")
    private Long REDIS_EXPIRE; // Redis 缓存过期时间（秒）
    @Value("${redis.key.admin}")
    private String REDIS_KEY_ADMIN; // Redis 缓存 key 前缀：用户信息
    @Value("${redis.key.resourceList}")
    private String REDIS_KEY_RESOURCE_LIST; // Redis 缓存 key 前缀：资源列表

    /**
     * 删除用户信息缓存
     * @param adminId
     */
    @Override
    public void delAdmin(Long adminId) {
        // 从数据库查询用户名
        UmsAdmin admin = adminService.getById(adminId);
        if (admin != null) {
            // 构建缓存 key
            String key = REDIS_DATABASE + ":" + REDIS_KEY_ADMIN + ":" + admin.getUsername();
            // 删除缓存
            redisService.del(key);
        }
    }

    /**
     * 删除单个用户的资源列表缓存
     * @param adminId
     */
    @Override
    public void delResourceList(Long adminId) {
        String key = REDIS_DATABASE + ":" + REDIS_KEY_RESOURCE_LIST + ":" + adminId;
        redisService.del(key);
    }

    /**
     * 删除拥有指定角色的所有用户的资源列表缓存
     * @param roleId
     */
    @Override
    public void delResourceListByRole(Long roleId) {
        QueryWrapper<UmsAdminRoleRelation> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdminRoleRelation::getRoleId,roleId);
        List<UmsAdminRoleRelation> relationList = adminRoleRelationService.list(wrapper);
        if (CollUtil.isNotEmpty(relationList)) {
            String keyPrefix = REDIS_DATABASE + ":" + REDIS_KEY_RESOURCE_LIST + ":";
            List<String> keys = relationList.stream().map(relation -> keyPrefix + relation.getAdminId()).collect(Collectors.toList());
            redisService.del(keys);
        }
    }

    /**
     * 删除拥有指定角色列表的所有用户的资源列表缓存（支持多个角色ID，适用于批量操作场景）
     * @param roleIds
     */
    @Override
    public void delResourceListByRoleIds(List<Long> roleIds) {
        QueryWrapper<UmsAdminRoleRelation> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(UmsAdminRoleRelation::getRoleId,roleIds);
        List<UmsAdminRoleRelation> relationList = adminRoleRelationService.list(wrapper);
        if (CollUtil.isNotEmpty(relationList)) {
            String keyPrefix = REDIS_DATABASE + ":" + REDIS_KEY_RESOURCE_LIST + ":";
            List<String> keys = relationList.stream().map(relation -> keyPrefix + relation.getAdminId()).collect(Collectors.toList());
            redisService.del(keys);
        }
    }

    /**
     * 删除拥有指定资源的所有用户的资源列表缓存
     * @param resourceId
     */
    @Override
    public void delResourceListByResource(Long resourceId) {
        List<Long> adminIdList = adminMapper.getAdminIdList(resourceId);
        if (CollUtil.isNotEmpty(adminIdList)) {
            String keyPrefix = REDIS_DATABASE + ":" + REDIS_KEY_RESOURCE_LIST + ":";
            List<String> keys = adminIdList.stream().map(adminId -> keyPrefix + adminId).collect(Collectors.toList());
            redisService.del(keys);
        }
    }

    /**
     * 从缓存中获取用户信息
     * @param username
     * @return
     */
    @Override
    public UmsAdmin getAdmin(String username) {
        String key = REDIS_DATABASE + ":" + REDIS_KEY_ADMIN + ":" + username;
        return (UmsAdmin) redisService.get(key);
    }

    /**
     * 将用户信息存入缓存
     * @param admin
     */
    @Override
    public void setAdmin(UmsAdmin admin) {
        String key = REDIS_DATABASE + ":" + REDIS_KEY_ADMIN + ":" + admin.getUsername();
        redisService.set(key, admin, REDIS_EXPIRE);
    }

    /**
     * 从缓存中获取用户的资源列表
     * @param adminId
     * @return
     */
    @Override
    public List<UmsResource> getResourceList(Long adminId) {
        String key = REDIS_DATABASE + ":" + REDIS_KEY_RESOURCE_LIST + ":" + adminId;
        return (List<UmsResource>) redisService.get(key);
    }

    /**
     * 将用户的资源列表存入缓存
     * @param adminId
     * @param resourceList
     */
    @Override
    public void setResourceList(Long adminId, List<UmsResource> resourceList) {
        String key = REDIS_DATABASE + ":" + REDIS_KEY_RESOURCE_LIST + ":" + adminId;
        redisService.set(key, resourceList, REDIS_EXPIRE);
    }
}
