package fr.esgi.color_run.servlet;

import java.io.*;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

/**
 * Servlet permettant de récupérer les informations de l'utilisateur actuellement authentifié
 */
@WebServlet(name = "meServlet", value = {"/me"})
public class MeServlet extends HttpServlet {
    private AuthService authService;

    public void init() {
        authService = new AuthServiceImpl();
    }

    /**
     * Cette méthode est appelée lorsqu'une requête GET est envoyée vers le servlet /me
     * Elle retourne les informations de l'utilisateur authentifié au format JSON
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Configuration de la réponse en JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Récupération du token depuis les attributs de la requête (placé par le filtre)
        String token = (String) request.getAttribute("jwt_token");

        if (token == null) {
            // Si l'utilisateur n'est pas authentifié, renvoyer un code 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            // Récupération des informations de l'utilisateur à partir du token
            Participant participant = authService.getParticipantFromToken(token);

            if (participant == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Informations utilisateur introuvables\"}");
                return;
            }

            // Récupération des rôles de l'utilisateur
            boolean isOrganisateur = (boolean) request.getAttribute("is_organisateur");
            boolean isAdmin = (boolean) request.getAttribute("is_admin");

            // Création d'un objet anonyme contenant les informations à retourner
            Object userData = new Object() {
                public final Participant user = participant;
                public final boolean organisateur = isOrganisateur;
                public final boolean admin = isAdmin;
            };

            String userText = "Utilisateur: " + participant.getNom() + " " + participant.getPrenom() +
                    "\nOrganisateur: " + isOrganisateur +
                    "\nAdmin: " + isAdmin
                    + "\nDonnées utilisateur: " + participant.toString();
            response.getWriter().write(userText);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur s'est produite lors de la récupération des données utilisateur\"}");
        }
    }
}
