package fr.esgi.color_run.filter;

import fr.esgi.color_run.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/auth/login", 
            "/api/auth/register",
            "/api/public"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Vérifier si le chemin est exclu de l'authentification
        String path = request.getRequestURI();
        if (isPathExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Vérifier le token JWT dans l'en-tête Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token manquant ou invalide");
            return;
        }
        
        String token = authHeader.substring(7);
        if (!JwtUtils.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token invalide ou expiré");
            return;
        }
        
        // Extraire les informations du token
        Claims claims = JwtUtils.parseToken(token);
        request.setAttribute("userId", claims.get("userId"));
        request.setAttribute("userRole", claims.get("role"));
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isPathExcluded(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
}
