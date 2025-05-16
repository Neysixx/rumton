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
    protected boolean isAuthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = (String) request.getAttribute("jwt_token");

        if (token == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        return true;
    }

    /**
     * Vérifie si l'utilisateur est un administrateur
     */
    protected boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getAttribute("is_admin") == null || !(boolean) request.getAttribute("is_admin")) {
            renderError(request, response, "Accès réservé aux administrateurs");
            return false;
        }
        return true;
    }

    /**
     * Vérifie si l'utilisateur est un organisateur
     */
    protected boolean isOrganisateur(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getAttribute("is_organisateur") == null || !(boolean) request.getAttribute("is_organisateur")) {
            renderError(request, response, "Accès réservé aux organisateurs");
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
    protected Admin getAuthenticatedAdmin(HttpServletRequest request) {
        String token = (String) request.getAttribute("jwt_token");
        return authService.getAdminFromToken(token);
    }

    /**
     * Rendu d'un template Thymeleaf
     */
    protected void renderTemplate(HttpServletRequest request, HttpServletResponse response, String templateName, Context context) throws IOException {
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        response.setContentType("text/html;charset=UTF-8");
        templateEngine.process(templateName, context, response.getWriter());
    }

    /**
     * Affiche une page d'erreur
     */
    protected void renderError(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        Context context = new Context();
        context.setVariable("error", errorMessage);
        renderTemplate(request, response, "error", context);
    }
}