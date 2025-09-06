package com.refconstructionopc.auth.security.filter;



import com.refconstructionopc.auth.security.service.JwtService;
import com.refconstructionopc.auth.security.service.UserDetailsAdapter;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserDetailsAdapter userDetailsAdapter;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsAdapter userDetailsAdapter) {
        this.jwtService = jwtService; this.userDetailsAdapter = userDetailsAdapter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.extractAllClaims(token);
                String email = claims.getSubject();
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var userDetails = userDetailsAdapter.loadUserByUsername(email);
                    if (jwtService.validateToken(token, userDetails)) {
                        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.info("Authenticated {} with authorities {}", email, auth.getAuthorities());
                    }
                    else{
                        log.warn("Token failed validation for {}", email);
                    }
                }
            } catch (Exception ignored) { /* invalid/expired token -> continue unauthenticated */ }
        }
        chain.doFilter(req, res);
    }
}
