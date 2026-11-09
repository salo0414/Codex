package com.fitflow.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
/** 拦截未登录的业务 API 请求；登录、会话检查和退出接口由控制器自行处理。 */
public class AuthenticationFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return (
      !path.startsWith("/api/") ||
      path.equals("/api/auth/login") ||
      path.equals("/api/auth/me") ||
      path.equals("/api/auth/logout")
    );
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  ) throws ServletException, IOException {
    var session = request.getSession(false);
    if (
      session == null ||
      !Boolean.TRUE.equals(session.getAttribute("fitflowAuthenticated"))
    ) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"message\":\"请先登录\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}
