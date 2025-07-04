package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet de base pour les servlets web utilisant Thymeleaf
 * Fournit des méthodes utilitaires pour l'authentification et le rendu des
 * templates
 */
public abstract class BaseWebServlet extends HttpServlet {

    protected AuthService authService;

    @Override
    public void init() {
        authService = new AuthServiceImpl();
    }

    /**
     * Vérifie si l'utilisateur est authentifié
     * 
     * @return true si l'utilisateur est authentifié, false sinon
     */
    public boolean isAuthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = (String) request.getAttribute("jwt_token");
        return token != null;
    }

    /**
     * Vérifie si l'utilisateur est un administrateur
     */
    protected boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getAttribute("is_admin") == null || !(boolean) request.getAttribute("is_admin")) {
            return false;
        }
        return true;
    }

    /**
     * Vérifie si l'utilisateur est un organisateur
     */
    protected boolean isOrganisateur(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getAttribute("is_organisateur") == null || !(boolean) request.getAttribute("is_organisateur")) {
            return false;
        }
        return true;
    }

    /**
     * Récupère le participant actuellement connecté
     */
    protected Participant getAuthenticatedParticipant(HttpServletRequest request) {
        String token = (String) request.getAttribute("jwt_token");
        return authService.getParticipantFromToken(token);
    }

    /**
     * Récupère l'administrateur actuellement connecté
     */
    public Admin getAuthenticatedAdmin(HttpServletRequest request) {
        String token = (String) request.getAttribute("jwt_token");
        return authService.getAdminFromToken(token);
    }

    /**
     * Rendu d'un template Thymeleaf
     */
    public void renderTemplate(HttpServletRequest request, HttpServletResponse response, String templateName, Context context) throws IOException, ServletException {
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        response.setContentType("text/html;charset=UTF-8");
        boolean isAuthValue = this.isAuthenticated(request, response);
        boolean isAdminValue = this.isAdmin(request, response);
        boolean isOrgaValue = this.isOrganisateur(request, response);
        Participant participant = getAuthenticatedParticipant(request);
        Admin admin = getAuthenticatedAdmin(request);
        context.setVariable("isAuth", isAuthValue);
        context.setVariable("isAdmin", isAdminValue);
        context.setVariable("isOrga", isOrgaValue);
        context.setVariable("participant", participant);
        context.setVariable("admin", admin);
        templateEngine.process(templateName, context, response.getWriter());
    }

    /**
     * Affiche une page d'erreur
     */
    public void renderError(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException, ServletException {
        Context context = new Context();
        context.setVariable("error", errorMessage);
        renderTemplate(request, response, "error", context);
    }

    protected Map<String, String> parseUrlEncodedBody(String body) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                params.put(key, value);
            }
        }
        return params;
    }
}