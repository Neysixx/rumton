package fr.esgi.color_run.filter;

import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Filtre qui intercepte toutes les requêtes pour valider les tokens JWT
 */
@WebFilter(urlPatterns = {"/api/*"})
public class JwtAuthFilter implements Filter {

    private AuthService authService;
    private final List<String> PUBLIC_PATHS = Arrays.asList("/api/login", "/api/register");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Utilisation de l'AuthService qui utilise déjà l'instance unique de JwtUtil
        authService = new AuthServiceImpl();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Récupération du chemin de la requête
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        // Passer les requêtes pour les chemins publics
        if (isPublicPath(path) || httpRequest.getMethod().equals("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }

        // Récupération du token depuis le header Authorization ou le cookie
        String authHeader = httpRequest.getHeader("Authorization");
        String cookieToken = extractTokenFromCookies(httpRequest.getCookies());

        // Extraction du token
        String token = authService.extractToken(authHeader, cookieToken);

        if (token != null && authService.isTokenValid(token)) {
            // Stockage du token dans la requête pour les servlets
            httpRequest.setAttribute("jwt_token", token);

            // Ajout d'attributs utilisateur pour faciliter l'accès
            httpRequest.setAttribute("user_id", authService.getUserIdFromToken(token));
            httpRequest.setAttribute("is_admin", authService.isAdmin(token));
            httpRequest.setAttribute("is_organisateur", authService.isOrganisateur(token));

            // Continuation de la chaîne de filtres
            chain.doFilter(request, response);
        } else {
            // Requête non authentifiée
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Authentification requise\"}");
        }
    }

    @Override
    public void destroy() {
        // Rien à faire ici
    }

    /**
     * Vérifie si le chemin est public (ne nécessite pas d'authentification)
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Extrait le token JWT des cookies
     */
    private String extractTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }

        Optional<Cookie> jwtCookie = Arrays.stream(cookies)
                .filter(cookie -> "jwt_token".equals(cookie.getName()))
                .findFirst();

        return jwtCookie.map(Cookie::getValue).orElse(null);
    }
}
