package com.coachkonnects.backend.config;

import com.coachkonnects.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if (uri.equals("/api/classes") || uri.startsWith("/api/classes/")) {
                return true;
            }
            if (uri.startsWith("/api/profile/") && !uri.endsWith("/me")) {
                return true;
            }
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtUtil.validateTokenWithoutEmailCheck(token)) {
                    // Token is valid, set attributes for controllers to use
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);

                    request.setAttribute("userEmail", email);
                    request.setAttribute("userRole", role);

                    // If hitting an admin route, ensure they have the ADMIN role
                    if (request.getRequestURI().startsWith("/api/admin")) {
                        if (!"ADMIN".equals(role)) {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Admin access required.");
                            return false;
                        }
                    }

                    return true;
                }
            } catch (Exception e) {
                // Token validation failed (expired, tampered, etc.)
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid or expired token.");
                return false;
            }
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized: Missing or invalid Authorization header.");
        return false;
    }
}
