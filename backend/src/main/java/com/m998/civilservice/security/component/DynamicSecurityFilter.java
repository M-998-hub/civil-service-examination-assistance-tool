package com.m998.civilservice.security.component;

import com.m998.civilservice.security.config.IgnoreUrlsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.security.web.FilterInvocation;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 动态权限过滤器，用于实现基于路径的动态权限过滤
 *
 * 功能说明：Spring Security 权限验证的入口过滤器
 */
public class DynamicSecurityFilter extends AbstractSecurityInterceptor implements Filter {

    // 动态权限元数据源，负责根据请求URL获取需要的权限列表
    @Autowired
    private DynamicSecurityMetadataSource dynamicSecurityMetadataSource;
    // 白名单配置
    @Autowired
    private IgnoreUrlsConfig ignoreUrlsConfig;

    /**
     * 设置权限决策管理器
     * @param dynamicAccessDecisionManager
     */
    @Autowired
    public void setMyAccessDecisionManager(DynamicAccessDecisionManager dynamicAccessDecisionManager) {
        super.setAccessDecisionManager(dynamicAccessDecisionManager);
    }

    /**
     * 过滤器初始化方法
     * @param filterConfig
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * 过滤器核心方法：拦截请求并进行权限验证
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        // 封装请求为 FilterInvocation（Spring Security 标准对象）
        FilterInvocation fi = new FilterInvocation(servletRequest, servletResponse, filterChain);



        //OPTIONS请求是跨域预检请求，浏览器自动发送，直接放行，否则跨域请求会失败
        if(request.getMethod().equals(HttpMethod.OPTIONS.toString())){
            fi.getChain().doFilter(fi.getRequest(), fi.getResponse());
            return;
        }
        //白名单请求直接放行
        PathMatcher pathMatcher = new AntPathMatcher();
        for (String path : ignoreUrlsConfig.getUrls()) {
            // Ant 路径匹配（支持通配符）
            if(pathMatcher.match(path,request.getRequestURI())){
                fi.getChain().doFilter(fi.getRequest(), fi.getResponse());
                return;
            }
        }
        //此处会调用AccessDecisionManager中的decide方法进行鉴权操作
        InterceptorStatusToken token = super.beforeInvocation(fi);
        try {
            // 权限验证通过，继续执行后续业务逻辑
            fi.getChain().doFilter(fi.getRequest(), fi.getResponse());
        } finally {
            // 无论是否发生异常，最后都要清理资源
            super.afterInvocation(token, null);
        }
    }

    /**
     * 过滤器销毁方法
     */
    @Override
    public void destroy() {
    }

    /**
     * 获取安全对象类型，用于确定拦截器能处理的对象类型
     * @return
     */
    @Override
    public Class<?> getSecureObjectClass() {
        return FilterInvocation.class;
    }

    /**
     * 获取安全元数据源，告诉Spring Security使用哪个元数据源获取权限信息
     * @return
     */
    @Override
    public SecurityMetadataSource obtainSecurityMetadataSource() {
        return dynamicSecurityMetadataSource;
    }

}
