package fr.esgi.color_run.servlet;

import java.io.*;

import fr.esgi.color_run.business.Participant;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;

/**
 * Servlet permettant d'afficher les informations de l'utilisateur connecté
 */
@WebServlet(name = "meServlet", value = {"/me"})
public class MeServlet extends BaseWebServlet {

    /**
     * Cette méthode est appelée lorsqu'une requête GET est envoyée vers le servlet /me
     * Elle affiche les informations de l'utilisateur authentifié via Thymeleaf
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            // Récupération des informations de l'utilisateur à partir du token
            Participant participant = getAuthenticatedParticipant(request);

            if (participant == null) {
                renderError(request, response, "Informations utilisateur introuvables");
                return;
            }

            // Ne pas exposer le mot de passe
            participant.setMotDePasse(null);

            // Préparation du contexte pour Thymeleaf
            Context context = new Context();
            context.setVariable("participant", participant);
            context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));
            context.setVariable("isAdmin", request.getAttribute("is_admin"));

            // Rendu de la page
            renderTemplate(request, response, "user_profile", context);

        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur s'est produite lors de la récupération des données utilisateur");
        }
    }
}