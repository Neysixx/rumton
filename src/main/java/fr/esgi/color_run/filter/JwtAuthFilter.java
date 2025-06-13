package fr.esgi.color_run.filter;

import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.util.DebugUtil;
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
@WebFilter(urlPatterns = {"/*"})
public class JwtAuthFilter implements Filter {

    private AuthService authService;
    private final List<String> PUBLIC_PATHS = Arrays.asList("/login", "/register", "/verify","/assets", "/favicon.ico", "/verify/resend", "/verify", "/mot-de-passe-oublie");

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

            String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

            // Récupération du token depuis le header ou les cookies
            String authHeader = httpRequest.getHeader("Authorization");
            String cookieToken = extractTokenFromCookies(httpRequest.getCookies());
            String token = authService.extractToken(authHeader, cookieToken);

            // Si un token est présent et valide, on injecte les attributs utilisateur
            if (token != null && authService.isTokenValid(token)) {
                httpRequest.setAttribute("jwt_token", token);
                httpRequest.setAttribute("user_id", authService.getUserIdFromToken(token));
                httpRequest.setAttribute("user_email", authService.getEmailFromToken(token));
                httpRequest.setAttribute("is_admin", authService.isAdmin(token));
                httpRequest.setAttribute("is_organisateur", authService.isOrganisateur(token));
                httpRequest.setAttribute("is_verified", authService.isVerified(token));
            }

            // Si la route est publique ou OPTIONS → on continue même sans token
            if (isPublicPath(path) || httpRequest.getMethod().equals("OPTIONS")) {
                // Si /login et que le token est valide et que le participant est vérifié, on redirige vers la page d'accueil
                if ((path.equals("/login") || path.equals("/register")) && token != null && authService.isTokenValid(token) && authService.isVerified(token)) {
                    httpResponse.sendRedirect(httpRequest.getContextPath() + "/courses");
                    return;
                }
                chain.doFilter(httpRequest, httpResponse);
                return;
            }

            // Si le token est valide et que le participant n'est pas vérifié, on redirige vers la page de vérification
            if (!authService.isVerified(token) && !isPublicPath(path)) {
                DebugUtil.log(this.getClass(), "Redirection vers la page de vérification : " + authService.getEmailFromToken(token));
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/verify?email=" + authService.getEmailFromToken(token));
                return;
            }

            // Sinon (route protégée), il faut un token valide
            if (token != null && authService.isTokenValid(token)) {
                chain.doFilter(httpRequest, httpResponse);
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
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
        return "/".equals(path) ||
                PUBLIC_PATHS.stream()
                        .anyMatch(path::startsWith);
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
