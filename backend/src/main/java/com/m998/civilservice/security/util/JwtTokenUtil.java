package com.m998.civilservice.security.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token 工具类
 *
 * 功能说明：负责 JWT Token 的完整生命周期管理
 */
public class JwtTokenUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenUtil.class);

    // JWT标准字段名称
    private static final String CLAIM_KEY_USERNAME = "sub"; // 主题（用户名）
    private static final String CLAIM_KEY_CREATED = "created"; // 创建时间
    private static final String CLAIM_KEY_JTI = "jti";
    private static final String CLAIM_KEY_TYPE = "type";

    // Token 类型常量
    private static final String TOKEN_TYPE_ACCESS = "access"; // 访问令牌
    private static final String TOKEN_TYPE_REFRESH = "refresh"; // 刷新令牌

    // 从配置文件注入
    @Value("${jwt.secret}")
    private String secret; // 签名密钥
    @Value("${jwt.expiration}")
    private Long expiration; // 访问令牌过期数据
    @Value("${jwt.refreshExpiration}")
    private Long refreshExpiration; // 刷新令牌过期时间
    @Value("${jwt.tokenHead}")
    private String tokenHead; // Token 前缀

    /**
     * 生成访问令牌
     * @param userDetails
     * @return
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USERNAME, userDetails.getUsername());
        claims.put(CLAIM_KEY_CREATED, new Date());
        claims.put(CLAIM_KEY_JTI, UUID.randomUUID().toString());
        claims.put(CLAIM_KEY_TYPE, TOKEN_TYPE_ACCESS);
        return buildToken(claims, expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USERNAME, userDetails.getUsername());
        claims.put(CLAIM_KEY_CREATED, new Date());
        claims.put(CLAIM_KEY_JTI, UUID.randomUUID().toString());
        claims.put(CLAIM_KEY_TYPE, TOKEN_TYPE_REFRESH);
        return buildToken(claims, refreshExpiration);
    }

    /**
     * 构建 JWT Token（内部方法）
     * @param claims
     * @param expireSeconds
     * @return
     */
    private String buildToken(Map<String, Object> claims, Long expireSeconds) {
        return Jwts.builder()
                .setClaims(claims) // 设置 Payload
                .setExpiration(new Date(System.currentTimeMillis() + expireSeconds * 1000)) // 过期时间
                .signWith(SignatureAlgorithm.HS512, secret) // 签名
                .compact(); // 生成 Token
    }

    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            LOGGER.info("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }

    public String getUserNameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getJti(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get(CLAIM_KEY_JTI, String.class) : null;
    }

    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get(CLAIM_KEY_TYPE, String.class) : null;
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    public Date getExpiredDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    public boolean isTokenExpired(String token) {
        Date exp = getExpiredDateFromToken(token);
        return exp != null && exp.before(new Date());
    }

    public long getRemainingExpiration(String token) {
        Date exp = getExpiredDateFromToken(token);
        if (exp == null) return 0;
        long remain = (exp.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remain, 0);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) return false;
        String username = claims.getSubject();
        return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String refreshHeadToken(String oldToken) {
        if (StrUtil.isEmpty(oldToken)) return null;
        String token = oldToken.substring(tokenHead.length());
        if (StrUtil.isEmpty(token)) return null;
        Claims claims = getClaimsFromToken(token);
        if (claims == null || isTokenExpired(token)) return null;
        if (tokenRefreshJustBefore(token, 30 * 60)) return token;
        claims.put(CLAIM_KEY_CREATED, new Date());
        return buildToken(claims, expiration);
    }

    /**
     * 检查 Token 是否在可刷新时间内
     * @param token
     * @param time
     * @return
     */
    private boolean tokenRefreshJustBefore(String token, int time) {
        Claims claims = getClaimsFromToken(token);
        Date created = claims.get(CLAIM_KEY_CREATED, Date.class);
        Date now = new Date();
        return now.after(created) && now.before(DateUtil.offsetSecond(created, time));
    }
}
