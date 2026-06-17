package com.danovich.platform.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Enforces {@code Authorization: Bearer <jwt>} on protected paths. Lightweight
 * servlet filter — no Spring Security — matching NearMe's approach.
 *
 * <p>Two things are deliberately left to the project rather than hardcoded:
 * <ul>
 *   <li><b>protectedPrefix</b> — which paths require a token (e.g. {@code /api/}).</li>
 *   <li><b>publicPaths</b> — exact paths that bypass auth even under the prefix.
 *       The token-exchange endpoint MUST be here, or no one could ever get a
 *       token. CORS preflight (OPTIONS) is always exempt automatically.</li>
 * </ul>
 *
 * <p>Register this as a high-priority filter (see the Spring autoconfiguration,
 * or add a {@code FilterRegistrationBean} with low order). On rejection it writes
 * a minimal JSON 401 and stops the chain.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final String protectedPrefix;
    private final List<String> publicPaths;

    public JwtAuthFilter(JwtService jwtService, String protectedPrefix, List<String> publicPaths) {
        this.jwtService = jwtService;
        this.protectedPrefix = protectedPrefix == null ? "/api/" : protectedPrefix;
        this.publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isProtected = path.startsWith(protectedPrefix);
        boolean isPublic = publicPaths.contains(path) || "OPTIONS".equalsIgnoreCase(request.getMethod());

        if (!isProtected || isPublic) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractBearer(request.getHeader("Authorization"));
        if (!jwtService.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"unauthorized\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String extractBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length()).trim();
    }
}
