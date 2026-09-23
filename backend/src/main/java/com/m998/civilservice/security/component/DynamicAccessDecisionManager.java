package com.m998.civilservice.security.component;

import cn.hutool.core.collection.CollUtil;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Iterator;

/**
 * 动态权限决策管理器，用于判断用户是否有访问权限
 */
public class DynamicAccessDecisionManager implements AccessDecisionManager {

    /**
     * 决策方法：判断当前用户是否有权限访问目标资源
     * @param authentication 当前用户的认证信息（包含用户拥有的权限列表）
     * @param object 被访问的目标对象（通常是方法或URL）
     * @param configAttributes 访问该资源需要的权限列表
     * @throws AccessDeniedException 权限不足时抛出（返回403）
     * @throws InsufficientAuthenticationException 认证不足时抛出（返回401）
     */
    @Override
    public void decide(Authentication authentication, Object object,
                       Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {
        // 如果该接口不需要任何权限，直接放行（公开资源）
        if (CollUtil.isEmpty(configAttributes)) {
            return;
        }

        // 遍历该接口需要的所有权限
        Iterator<ConfigAttribute> iterator = configAttributes.iterator();
        while (iterator.hasNext()) {
            ConfigAttribute configAttribute = iterator.next();

            // 获取需要的权限标识
            String needAuthority = configAttribute.getAttribute();

            // 遍历当前用户拥有的所有权限，检查是否匹配
            for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
                // 权限匹配判断：去除首尾空格后比较
                if (needAuthority.trim().equals(grantedAuthority.getAuthority())) {
                    // 匹配成功后放行
                    return;
                }
            }
        }
        throw new AccessDeniedException("抱歉，您没有访问权限");
    }

    /**
     * 判断当前决策管理器是否支持指定的配置属性
     * @param configAttribute
     * @return
     */
    @Override
    public boolean supports(ConfigAttribute configAttribute) {
        return true; // 支持所有类型的配置属性
    }

    /**
     * 判断当前决策管理器是否支持指定类型的类
     * @param aClass
     * @return
     */
    @Override
    public boolean supports(Class<?> aClass) {
        return true; //支持所有类型的类
    }

}
