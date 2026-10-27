package com.tpobuddy.security;

import com.tpobuddy.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());

        // Skip static assets, public views, and auth endpoints
        if (path.startsWith("/assets/") || path.startsWith("/react-dist/") || path.equals("/") || path.equals("/index.jsp")
                || path.equals("/login") || path.equals("/login.jsp")
                || path.equals("/register") || path.equals("/register.jsp")
                || path.equals("/forgot-password") || path.equals("/forgot-password.jsp") || path.startsWith("/api/auth/")
                || path.equals("/react-portal") || path.equals("/react-portal.jsp") || path.equals("/portal")
                || path.equals("/react-app")
                || path.equals("/api/jobs/list") || path.equals("/api/notifications")
                || path.equals("/api/student/metrics") || path.startsWith("/api/tests/questions")
                || path.equals("/api/snjb/tests") || path.equals("/api/snjb/categories")
                || path.equals("/api/snjb/student/summary") || path.equals("/api/snjb/active-session")
                || path.equals("/logout") || path.startsWith("/actuator/")) {
            return true;
        }

        // Apply no-cache headers to all protected views and APIs
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (user == null) {
            if (path.startsWith("/api/")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().write("{\"success\":false,\"error\":\"Authentication required\"}");
                return false;
            }
            res.sendRedirect(contextPath + "/login.jsp?error=unauthorized");
            return false;
        }

        // Faculty authorization check
        if ((path.startsWith("/faculty") || path.startsWith("/api/faculty/") || path.startsWith("/api/snjb/admin/") || path.startsWith("/api/admin/"))
                && !user.isTeacher() && !user.isAdmin()) {
            if (path.startsWith("/api/")) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().write("{\"success\":false,\"error\":\"Faculty or Administrator access required\"}");
                return false;
            }
            res.sendRedirect(contextPath + "/dashboard?error=forbidden");
            return false;
        }

        // Administrator authorization check
        if (path.startsWith("/admin") && !user.isAdmin()) {
            if (path.startsWith("/api/")) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().write("{\"success\":false,\"error\":\"Administrator access required\"}");
                return false;
            }
            res.sendRedirect(contextPath + "/dashboard?error=forbidden");
            return false;
        }

        return true;
    }
}
