package com.geek.server.filter;

import com.geek.common.exception.ServiceException;
import com.geek.server.domain.MarkOpenApiPrincipal;
import com.geek.server.service.MarkOpenApiCredentialService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component("markOpenApiAuthenticationFilter")
public class MarkOpenApiAuthenticationFilter extends OncePerRequestFilter {
    private final MarkOpenApiCredentialService service;
    public MarkOpenApiAuthenticationFilter(@Lazy MarkOpenApiCredentialService service) {
        this.service = service;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/open/v1/"); }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        MarkOpenApiPrincipal principal;
        try {
            principal = service.authenticate(request.getHeader("X-API-Key"), request.getHeader("X-API-Secret"));
        } catch (ServiceException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
            return;
        }
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal.getLoginUser(), null, principal.getLoginUser().getAuthorities());
        token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(token);
        request.setAttribute("markOpenApiPrincipal", principal);
        chain.doFilter(request, response);
    }
}
