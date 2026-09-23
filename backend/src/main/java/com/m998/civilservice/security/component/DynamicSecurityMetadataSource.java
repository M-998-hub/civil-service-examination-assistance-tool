package com.m998.civilservice.security.component;

import cn.hutool.core.util.URLUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 动态权限数据源，用于获取动态权限规则
 *
 * 功能说明：告诉 Spring Security “这个URL需要什么权限才能访问”
 *
 * 核心职责：
 * 1. 从数据库/配置加载所有权限规则（URL -> 所需权限）
 * 2. 根据请求的URL，匹配对应的权限规则
 * 3. 返回匹配的权限列表给 AccessDecisionManager 进行验证
 */
public class DynamicSecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

    // 权限规则缓存（URL -> 所需权限）
    private static Map<String, ConfigAttribute> configAttributeMap = null;

    // 动态权限服务，负责从数据库或其他数据源加载权限规则
    @Autowired
    private DynamicSecurityService dynamicSecurityService;

    // 加载权限数据源（在 Bean 初始化后执行）
    @PostConstruct  // Spring 生命周期回调，在依赖注入完成后执行
    public void loadDataSource() {
        // 调用 DynamicSecurityService 加载权限数据
        configAttributeMap = dynamicSecurityService.loadDataSource();
    }

    /**
     * 清理权限数据源（用于刷新缓存）
     */
    public void clearDataSource() {
        configAttributeMap.clear();
        configAttributeMap = null;
    }

    /**
     * 获取访问指定 URL 需要的权限列表
     * @param o
     * @return
     * @throws IllegalArgumentException
     */
    @Override
    public Collection<ConfigAttribute> getAttributes(Object o) throws IllegalArgumentException {

        // 如果换成为空，重新加载权限数据
        if (configAttributeMap == null) this.loadDataSource();

        // 准备返回的权限列表
        List<ConfigAttribute>  configAttributes = new ArrayList<>();

        // 获取当前请求的 URL
        String url = ((FilterInvocation) o).getRequestUrl();

        // 去除 URL 中的参数部分
        String path = URLUtil.getPath(url);

        // 使用 Ant 路径匹配器
        PathMatcher pathMatcher = new AntPathMatcher();
        Iterator<String> iterator = configAttributeMap.keySet().iterator();

        // 遍历所有权限规则，查找匹配的 URL 模式
        while (iterator.hasNext()) {
            String pattern = iterator.next();

            // 路径匹配：判断请求 URL 是否匹配当前规则
            if (pathMatcher.match(pattern, path)) {
                // 匹配成功就将该规则对应的权限加入列表
                configAttributes.add(configAttributeMap.get(pattern));
            }
        }
        // 返回权限列表（可能为空，表示不需要权限）
        return configAttributes;
    }

    /**
     * 获取所有配置属性（在特定场景下使用，本实现不需要，返回null）
     * @return
     */
    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    /**
     * 判断是否支持指定类型的类
     * @param aClass
     * @return
     */
    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }

}
