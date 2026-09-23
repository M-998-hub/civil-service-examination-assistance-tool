package com.m998.civilservice.security.component;

import com.m998.civilservice.common.service.RedisService;
import com.m998.civilservice.security.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * 功能说明：拦截每个请求，解析 JWT Token 进行身份认证
 */
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);

    // Token 黑名单前缀（Redis Key），用户退出登录后，Token被加入黑名单，无法再使用
    private static final String BLACKLIST_PREFIX = "blacklist:access:";

    // 用户详情服务，用于根据用户名加载用户信息（含权限）
    @Autowired
    private UserDetailsService userDetailsService;

    // JWT Token工具类，用于解析、验证、生成Token
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // Redis 服务，用于黑名单检查
    @Autowired
    private RedisService redisService;

    // Token 所在的请求头名称
    @Value("${jwt.tokenHeader}")
    private String tokenHeader;

    // Token前缀，默认Bearer
    @Value("${jwt.tokenHead}")
    private String tokenHead;

    /**
     * 过滤器核心方法：在每个请求中执行 JWT 认证
     * @param request
     * @param response
     * @param chain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 从请求体获取 Token
        String authHeader = request.getHeader(this.tokenHeader);
        if (authHeader != null && authHeader.startsWith(this.tokenHead)) {

            // 提取 Token（去除前缀）
            String authToken = authHeader.substring(this.tokenHead.length());

            // 获取Token 的唯一标识（JTI）
            String jti = jwtTokenUtil.getJti(authToken);
            if (jti != null && redisService.hasKey(BLACKLIST_PREFIX + jti)) {
                // Token 在黑名单中（用户已退出登录）
                LOGGER.info("token in blacklist: jti={}", jti);
                // 不设置认证，继续执行后续过滤器，最终会被权限验证拦截，返回401
                chain.doFilter(request, response);
                return;
            }

            // 从 Token 解析用户名
            String username = jwtTokenUtil.getUserNameFromToken(authToken);
            LOGGER.info("checking username:{}", username);

            // 检查是否需要设置认证
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 从数据库加载用户信息
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 检查 Token 是否过期、签名是否正确、是否匹配用户信息
                if (jwtTokenUtil.validateToken(authToken, userDetails)) {

                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, // 用户信息
                                    null, // 凭证（密码已不需要）
                                    userDetails.getAuthorities()); // 用户权限

                    // 设置请求详情，包含IP地址，Session ID等
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    LOGGER.info("authenticated user:{}", username);

                    // 将认证信息放入 SecurityContext。后续的权限验证（AccessDecisionManager）会从这里获取用户信息
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // 继续执行后续过滤器
        chain.doFilter(request, response);
    }
}
