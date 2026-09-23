package com.m998.civilservice.config;

import com.m998.civilservice.modules.auth.model.UmsResource;
import com.m998.civilservice.modules.auth.service.UmsAdminService;
import com.m998.civilservice.modules.auth.service.UmsResourceService;
import com.m998.civilservice.security.component.DynamicSecurityService;
import com.m998.civilservice.security.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用认证与动态权限配置。
 */
@Configuration
public class ApplicationSecurityConfig {

    @Autowired
    private UmsAdminService adminService;
    @Autowired
    private UmsResourceService resourceService;

    @Bean
    public UserDetailsService userDetailsService() {
        //获取登录用户信息
        return username -> adminService.loadUserByUsername(username);
    }

    @Bean
    public DynamicSecurityService dynamicSecurityService() {
        //创建DynamicSecurityService接口的匿名实现类对象
        return new DynamicSecurityService() {
            //实现接口中的方法，用于加载权限数据源
            @Override
            public Map<String, ConfigAttribute> loadDataSource() {
                //创建线程安全的Map，存储URL -> 权限的映射关系
                Map<String, ConfigAttribute> map = new ConcurrentHashMap<>();
                //从数据库查询所有资源
                List<UmsResource> resourceList = resourceService.list();
                for (UmsResource resource : resourceList) {
                    //将URL作为key，权限配置对象作为value存入Map
                    map.put(resource.getUrl(), new org.springframework.security.access.SecurityConfig(resource.getId() + ":" + resource.getName()));
                }
                //返回构建好的权限映射表
                return map;
            }
        };
    }
}
