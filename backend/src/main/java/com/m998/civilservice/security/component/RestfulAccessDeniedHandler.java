package com.m998.civilservice.security.component;

import cn.hutool.json.JSONUtil;
import com.m998.civilservice.common.api.CommonResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义返回结果：没有权限访问时
 *
 * 在 Spring Security中，AccessDeniedHandler 是访问拒绝处理器
 * 当用户已登录但没有足够权限访问某个资源时，会调用此处理器
 */
public class RestfulAccessDeniedHandler implements AccessDeniedHandler{
    /**
     * 处理访问拒绝异常，返回统一的 JSON 响应
     * @param request
     * @param response
     * @param e
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException e) throws IOException, ServletException {
        // 设置跨域响应头，允许所有域名访问
        response.setHeader("Access-Control-Allow-Origin", "*");

        // 设置缓存控制，禁止浏览器缓存错误页面，确保每次都获取最新信息
        response.setHeader("Cache-Control","no-cache");

        // 设置响应编码和内容类型
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        // 返回 JSON 格式的错误消息
        response.getWriter().println(JSONUtil.parse(CommonResult.forbidden(e.getMessage())));

        // 确保所有数据都已写入响应
        response.getWriter().flush();
    }
}
