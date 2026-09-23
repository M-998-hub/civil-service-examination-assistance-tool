package com.m998.civilservice.domain;

import com.m998.civilservice.modules.auth.model.UmsAdmin;
import com.m998.civilservice.modules.auth.model.UmsResource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SpringSecurity需要的用户详情
 */
public class AdminUserDetails implements UserDetails {
    private UmsAdmin umsAdmin;
    private List<UmsResource> resourceList;
    public AdminUserDetails(UmsAdmin umsAdmin, List<UmsResource> resourceList) {
        this.umsAdmin = umsAdmin;
        this.resourceList = resourceList;
    }

    public UmsAdmin getUmsAdmin() {
        return umsAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //返回当前用户的权限
        //支持三种格式：id:name、name、url
        return resourceList.stream()
                .flatMap(resource -> {
                    java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

                    // 添加 id:name 格式（与 DynamicSecurityMetadataSource 格式一致）
                    if (resource.getId() != null && resource.getName() != null) {
                        authorities.add(new SimpleGrantedAuthority(resource.getId() + ":" + resource.getName()));
                    }

                    // 添加资源名称作为权限
                    if (resource.getName() != null) {
                        authorities.add(new SimpleGrantedAuthority(resource.getName()));
                    }

                    // 添加资源URL作为权限
                    if (resource.getUrl() != null) {
                        authorities.add(new SimpleGrantedAuthority(resource.getUrl()));
                    }

                    return authorities.stream();
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return umsAdmin.getPassword();
    }

    @Override
    public String getUsername() {
        return umsAdmin.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return umsAdmin.getStatus().equals(1);
    }
}
