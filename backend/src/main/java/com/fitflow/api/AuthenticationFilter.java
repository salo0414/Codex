package com.fitflow.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.fitflow.repo.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
/** 拦截未登录的业务 API 请求；登录、会话检查和退出接口由控制器自行处理。 */
public class AuthenticationFilter extends OncePerRequestFilter {

  private final MemberRepository members;

  public AuthenticationFilter(MemberRepository members) {
    this.members = members;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return (
      !path.startsWith("/api/") ||
      path.equals("/api/auth/login") ||
      path.equals("/api/auth/register") ||
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
    if (!"ADMIN".equals(session.getAttribute("fitflowRole"))) {
      Long memberId = (Long) session.getAttribute("fitflowMemberId");
      if (memberId != null && members.findById(memberId).map(member -> "停用".equals(member.status)).orElse(true)) {
        session.invalidate();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"账户已停用或档案不可用，请联系管理员\"}");
        return;
      }
      String path = request.getRequestURI();
      String method = request.getMethod();
      boolean userRoute =
        ("GET".equals(method) && (path.equals("/api/classes") || path.equals("/api/profile") || path.equals("/api/bookings"))) ||
        ("PUT".equals(method) && path.equals("/api/profile")) ||
        ("POST".equals(method) && path.equals("/api/bookings")) ||
        ("DELETE".equals(method) && path.matches("/api/bookings/\\d+"));
      if (!userRoute) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"需要管理员权限\"}");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
