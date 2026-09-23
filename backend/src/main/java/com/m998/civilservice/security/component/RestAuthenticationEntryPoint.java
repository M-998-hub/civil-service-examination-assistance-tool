package com.m998.civilservice.security.component;

import cn.hutool.json.JSONUtil;
import com.m998.civilservice.common.api.CommonResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义返回结果：未登录或登录过期
 *
 * 功能说明：处理“认证不足（401）的统一响应
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    /**
     * 处理认证异常，返回统一的 JSON 响应
     * @param request
     * @param response
     * @param authException
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 设置跨域响应头，允许所有域名访问
        response.setHeader("Access-Control-Allow-Origin", "*");

        // 设置缓存控制，禁止浏览器缓存错误页面，确保每次都获取最新信息
        response.setHeader("Cache-Control","no-cache");

        // 设置响应编码和内容类型
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        // 返回 JSON 格式的错误消息
        response.getWriter().println(JSONUtil.parse(CommonResult.unauthorized(authException.getMessage())));

        // 确保所有数据都已写入响应
        response.getWriter().flush();
    }
}
