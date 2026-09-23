package com.m998.civilservice.modules.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.m998.civilservice.common.exception.Asserts;
import com.m998.civilservice.domain.AdminUserDetails;
import com.m998.civilservice.modules.auth.dto.UmsAdminParam;
import com.m998.civilservice.modules.auth.dto.UpdateAdminPasswordParam;
import com.m998.civilservice.modules.auth.mapper.UmsAdminLoginLogMapper;
import com.m998.civilservice.modules.auth.mapper.UmsAdminMapper;
import com.m998.civilservice.modules.auth.mapper.UmsResourceMapper;
import com.m998.civilservice.modules.auth.mapper.UmsRoleMapper;
import com.m998.civilservice.modules.auth.model.*;
import com.m998.civilservice.modules.auth.service.UmsAdminCacheService;
import com.m998.civilservice.modules.auth.service.UmsAdminRoleRelationService;
import com.m998.civilservice.modules.auth.service.UmsAdminService;
import com.m998.civilservice.security.util.JwtTokenUtil;
import com.m998.civilservice.security.util.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 后台管理员管理Service实现类
 *
 * 功能说明：管理系统用户的完整生命周期
 *
 * 核心功能：
 * 1.用户管理：注册、登录、查询、更新、删除
 * 2.权限管理：角色分配、资源（菜单/按钮）查询
 * 3.密码管理：加密存储、密码修改、密码验证
 * 4.缓存管理：用户信息和权限资源的缓存读写
 * 5.登录日志：记录登录时间、IP等信息
 */
@Service
public class UmsAdminServiceImpl extends ServiceImpl<UmsAdminMapper,UmsAdmin> implements UmsAdminService {
    // 日志记录器
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAdminServiceImpl.class);
    // JWT Token 工具类（用于生成和刷新登录令牌）
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    // 密码编码器（BCrypt），用于密码加密和匹配验证
    @Autowired
    private PasswordEncoder passwordEncoder;
    // 登录日志 Mapper
    @Autowired
    private UmsAdminLoginLogMapper loginLogMapper;
    // 用户-角色关联服务
    @Autowired
    private UmsAdminRoleRelationService adminRoleRelationService;
    // 角色 Mapper
    @Autowired
    private UmsRoleMapper roleMapper;
    // 资源 Mapper（菜单/按钮权限）
    @Autowired
    private UmsResourceMapper resourceMapper;

    /**
     * 根据用户名获取用户信息（带缓存）
     * @param username
     * @return
     */
    @Override
    public UmsAdmin getAdminByUsername(String username) {
        // 先从缓存获取
        UmsAdmin admin = getCacheService().getAdmin(username);
        if(admin!=null) return  admin;

        // 缓存未命中，查询数据库
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdmin::getUsername,username);
        List<UmsAdmin> adminList = list(wrapper);

        // 数据库查到，放入缓存后返回
        if (adminList != null && adminList.size() > 0) {
            admin = adminList.get(0);
            getCacheService().setAdmin(admin); // 写入缓存
            return admin;
        }

        // 用户不存在
        return null;
    }

    /**
     * 注册新用户
     * @param umsAdminParam
     * @return
     */
    @Override
    public UmsAdmin register(UmsAdminParam umsAdminParam) {
        // 创建用户对象，复制参数
        UmsAdmin umsAdmin = new UmsAdmin();
        BeanUtils.copyProperties(umsAdminParam, umsAdmin);
        umsAdmin.setCreateTime(new Date());
        umsAdmin.setStatus(1);

        //查询是否有相同用户名的用户
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdmin::getUsername,umsAdmin.getUsername());
        List<UmsAdmin> umsAdminList = list(wrapper);
        if (umsAdminList.size() > 0) {
            return null;
        }

        //将密码进行加密操作
        String encodePassword = passwordEncoder.encode(umsAdmin.getPassword());
        umsAdmin.setPassword(encodePassword);

        // 保存到数据库
        baseMapper.insert(umsAdmin);
        return umsAdmin;
    }

    /**
     * 登录认证
     * @param username 用户名
     * @param password 密码
     * @return
     */
    @Override
    public String login(String username, String password) {
        String token = null;

        //密码需要客户端加密后传递
        try {
            // 根据用户名获取用户信息（含权限）
            UserDetails userDetails = loadUserByUsername(username);

            // 密码匹配验证
            if(!passwordEncoder.matches(password,userDetails.getPassword())){
                Asserts.fail("密码不正确");
            }

            // 检查账号是否启用
            if(!userDetails.isEnabled()){
                Asserts.fail("帐号已被禁用");
            }

            // 创建 Spring Security 认证令牌（包含了用户信息和权限）
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 放入 SecurityContextHolder（当前线程的上下文）
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 生成 JWT token
            token = jwtTokenUtil.generateToken(userDetails);
//            updateLoginTimeByUsername(username);

            // 记录登录日志（IP、时间）
            insertLoginLog(username);
        } catch (AuthenticationException e) {
            LOGGER.warn("登录异常:{}", e.getMessage());
        }
        return token;
    }

    /**
     * 添加登录记录
     * @param username 用户名
     */
    private void insertLoginLog(String username) {
        // 获取用户信息
        UmsAdmin admin = getAdminByUsername(username);
        if(admin==null) return;

        // 构建登录日志
        UmsAdminLoginLog loginLog = new UmsAdminLoginLog();
        loginLog.setAdminId(admin.getId());
        loginLog.setCreateTime(new Date());

        // 获取客户端 IP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        loginLog.setIp(request.getRemoteAddr());

        // 保存日志
        loginLogMapper.insert(loginLog);
    }

    /**
     * 更新登录时间（已废弃，使用登录日志代替）
     */
    private void updateLoginTimeByUsername(String username) {
        UmsAdmin record = new UmsAdmin();
        record.setLoginTime(new Date());
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdmin::getUsername,username);
        update(record,wrapper);
    }

    /**
     * 刷新 JWT token
     * @param oldToken 旧的token
     * @return
     */
    @Override
    public String refreshToken(String oldToken) {
        return jwtTokenUtil.refreshHeadToken(oldToken);
    }

    /**
     * 分页查询用户列表
     * @param keyword
     * @param pageSize
     * @param pageNum
     * @return
     */
    @Override
    public Page<UmsAdmin> list(String keyword, Integer pageSize, Integer pageNum) {
        // 创建分页对象
        Page<UmsAdmin> page = new Page<>(pageNum,pageSize);
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        LambdaQueryWrapper<UmsAdmin> lambda = wrapper.lambda();

        // 搜索条件：用户名或昵称包含关键字
        if(StrUtil.isNotEmpty(keyword)){
            lambda.like(UmsAdmin::getUsername,keyword);
            lambda.or().like(UmsAdmin::getNickName,keyword);
        }

        // 执行分页查询
        return page(page,wrapper);
    }

    /**
     * 更新用户信息
     * @param id
     * @param admin
     * @return
     */
    @Override
    public boolean update(Long id, UmsAdmin admin) {
        // 告诉 MyBatis-Plus 要更新哪条记录
        admin.setId(id);

        // 获取原用户信息
        UmsAdmin rawAdmin = getById(id);

        // 处理密码更新
        if(rawAdmin.getPassword().equals(admin.getPassword())){
            //与原加密密码相同的不需要修改
            admin.setPassword(null);
        }else{
            //与原加密密码不同的需要加密修改
            if(StrUtil.isEmpty(admin.getPassword())){
                admin.setPassword(null);
            }else{
                admin.setPassword(passwordEncoder.encode(admin.getPassword()));
            }
        }

        // 执行更新
        boolean success = updateById(admin);

        // 删除缓存（用户信息变更）
        getCacheService().delAdmin(id);
        return success;
    }

    /**
     * 用户删除
     * @param id
     * @return
     */
    @Override
    public boolean delete(Long id) {
        // 删除缓存
        getCacheService().delAdmin(id);

        // 删除用户
        boolean success = removeById(id);

        // 删除资源缓存
        getCacheService().delResourceList(id);
        return success;
    }

    /**
     * 更新用户角色
     * @param adminId
     * @param roleIds
     * @return
     */
    @Override
    public int updateRole(Long adminId, List<Long> roleIds) {
        int count = roleIds == null ? 0 : roleIds.size();

        //先删除原来的关系
        QueryWrapper<UmsAdminRoleRelation> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdminRoleRelation::getAdminId,adminId);
        adminRoleRelationService.remove(wrapper);

        //建立新关系
        if (!CollectionUtils.isEmpty(roleIds)) {
            List<UmsAdminRoleRelation> list = new ArrayList<>();
            for (Long roleId : roleIds) {
                UmsAdminRoleRelation roleRelation = new UmsAdminRoleRelation();
                roleRelation.setAdminId(adminId);
                roleRelation.setRoleId(roleId);
                list.add(roleRelation);
            }
            adminRoleRelationService.saveBatch(list);
        }

        // 删除资源缓存（权限变更）
        getCacheService().delResourceList(adminId);
        return count;
    }

    /**
     * 获取用户的角色列表
     * @param adminId
     * @return
     */
    @Override
    public List<UmsRole> getRoleList(Long adminId) {
        return roleMapper.getRoleList(adminId);
    }

    /**
     * 获取用户的资源列表（菜单/按钮权限）
     * @param adminId
     * @return
     */
    @Override
    public List<UmsResource> getResourceList(Long adminId) {
        // 从缓存获取
        List<UmsResource> resourceList = getCacheService().getResourceList(adminId);
        if(CollUtil.isNotEmpty(resourceList)){
            return  resourceList;
        }

        // 从数据库查询
        resourceList = resourceMapper.getResourceList(adminId);

        // 放入缓存
        if(CollUtil.isNotEmpty(resourceList)){
            getCacheService().setResourceList(adminId,resourceList);
        }
        return resourceList;
    }

    /**
     * 修改密码
     * @param param
     * @return
     */
    @Override
    public int updatePassword(UpdateAdminPasswordParam param) {
        // 参数校验
        if(StrUtil.isEmpty(param.getUsername())
                ||StrUtil.isEmpty(param.getOldPassword())
                ||StrUtil.isEmpty(param.getNewPassword())){
            return -1;
        }

        // 查询用户
        QueryWrapper<UmsAdmin> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsAdmin::getUsername,param.getUsername());
        List<UmsAdmin> adminList = list(wrapper);
        if(CollUtil.isEmpty(adminList)){
            return -2;  // 用户不存在
        }
        UmsAdmin umsAdmin = adminList.get(0);

        // 验证旧密码
        if(!passwordEncoder.matches(param.getOldPassword(),umsAdmin.getPassword())){
            return -3; // 旧密码错误
        }

        // 更新密码
        umsAdmin.setPassword(passwordEncoder.encode(param.getNewPassword()));
        updateById(umsAdmin);

        // 删除缓存
        getCacheService().delAdmin(umsAdmin.getId());
        return 1; // 修改成功
    }

    /**
     * 加载用户信息（Spring Security 调用）
     * @param username
     * @return
     */
    @Override
    public UserDetails loadUserByUsername(String username){
        // 获取用户信息（带缓存）
        UmsAdmin admin = getAdminByUsername(username);
        if (admin != null) {

            // 获取用户权限资源
            List<UmsResource> resourceList = getResourceList(admin.getId());

            // 封装成 Spring Security 的用户详情对象
            return new AdminUserDetails(admin,resourceList);
        }
        throw new UsernameNotFoundException("用户名或密码错误");
    }

    /**
     * 获取缓存服务实例
     * @return
     */
    @Override
    public UmsAdminCacheService getCacheService() {
        // 使用 SpringUtil.getBean() 延迟获取 Bean，避免循环依赖
        return SpringUtil.getBean(UmsAdminCacheService.class);
    }
}
