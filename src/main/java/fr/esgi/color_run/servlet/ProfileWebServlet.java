package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Participant;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;

/**
 * Servlet pour afficher le profil utilisateur avec Thymeleaf
 */
@WebServlet(name = "profileWebServlet", value = "/web/profile")
public class ProfileWebServlet extends BaseWebServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            // Récupération du token et des informations utilisateur
            String token = (String) request.getAttribute("jwt_token");
            Participant participant = authService.getParticipantFromToken(token);

            if (participant == null) {
                renderError(request, response, "Informations utilisateur introuvables");
                return;
            }

            // Masquer le mot de passe
            participant.setMotDePasse(null);

            // Préparation du contexte pour Thymeleaf
            Context context = new Context();
            context.setVariable("participant", participant);
            context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));
            context.setVariable("isAdmin", request.getAttribute("is_admin"));

            // Rendu de la page
            renderTemplate(request, response, "profile", context);

        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur s'est produite lors de la récupération des données utilisateur");
        }
    }
} 