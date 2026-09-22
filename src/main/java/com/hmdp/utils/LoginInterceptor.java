package com.hmdp.utils;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public class LoginInterceptor implements HandlerInterceptor {

    /** Knife4j 4.x 及其他白名单路径(拦截器内部判断,绕过Spring Boot 3 PathPattern差异) */
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<String> WHITELIST = List.of(
            // 业务白名单
            "/shop/**", "/voucher/**", "/shop-type/**", "/upload/**",
            "/blog/hot", "/user/code", "/user/login",
            // Knife4j 4.x 资源
            "/doc.html", "/webjars/**", "/favicon.ico",
            "/swagger-ui.html", "/swagger-ui/**",
            "/swagger-resources", "/swagger-resources/**",
            "/v2/api-docs", "/v2/api-docs/**",
            "/v3/api-docs", "/v3/api-docs/**",
            // AI 健康检查(不需登录)
            "/chat/health",
            // 错误页
            "/error"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        // 命中白名单直接放行
        for (String pattern : WHITELIST) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        // 判断是否需要拦截(ThreadLocal中是否有用户)
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
