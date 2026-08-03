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
public class TokenInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public TokenInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token != null && token.contains("_")) {
            try {
                String userIdStr = token.split("_")[0];
                Long userId = Long.parseLong(userIdStr);
                request.setAttribute("userId", userId);
                UserContext.setUserId(userId);
                return true;
            } catch (NumberFormatException e) {
                writeErrorResponse(response, ResultCode.UNAUTHORIZED);
                return false;
            }
        }

        writeErrorResponse(response, ResultCode.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeErrorResponse(HttpServletResponse response, ResultCode code) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code)));
    }
}
