package com.m998.civilservice.modules.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.m998.civilservice.modules.auth.mapper.UmsAdminRoleRelationMapper;
import com.m998.civilservice.modules.auth.model.UmsAdminRoleRelation;
import com.m998.civilservice.modules.auth.service.UmsAdminRoleRelationService;
import org.springframework.stereotype.Service;

/**
 * 管理员角色关系管理Service实现类
 */
@Service
public class UmsAdminRoleRelationServiceImpl extends ServiceImpl<UmsAdminRoleRelationMapper, UmsAdminRoleRelation> implements UmsAdminRoleRelationService {
}
