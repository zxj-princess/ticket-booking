package com.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.context.UserContext;
import com.ticket.common.result.R;
import com.ticket.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements org.springframework.web.servlet.AsyncHandlerInterceptor {

    private final ObjectMapper objectMapper;
    @org.springframework.beans.factory.annotation.Autowired
    private SessionService sessions;
    @org.springframework.beans.factory.annotation.Autowired
    private com.ticket.mapper.UserMapper users;

    public TokenInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean read = "GET".equals(request.getMethod());
        if (path.equals("/user/login") || path.equals("/user/register") ||
            (read && (path.matches("/train/(search|list|[0-9]+)") || path.matches("/station/(list|search)")))) return true;
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        Long userId = sessions.resolve(token);
        com.ticket.entity.User user = userId == null ? null : users.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }
        boolean adminOnly = path.startsWith("/admin/") ||
            path.equals("/user/list") || path.equals("/user/status") ||
            path.matches("/user/[0-9]+") || path.equals("/order/list") ||
            path.startsWith("/order/repair") ||
            (!read && (path.startsWith("/train/") || path.startsWith("/station/")));
        if (adminOnly && !Integer.valueOf(1).equals(user.getRole())) {
            writeErrorResponse(response, ResultCode.FORBIDDEN);
            return false;
        }
        request.setAttribute("userId", userId);
        UserContext.setUserId(userId);
        return true;
    }

    @Override public void afterConcurrentHandlingStarted(HttpServletRequest request,HttpServletResponse response,Object handler) {UserContext.clear();}

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeErrorResponse(HttpServletResponse response, ResultCode code) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code.getCode());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code)));
    }
}
