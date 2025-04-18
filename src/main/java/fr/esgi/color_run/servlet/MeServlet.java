package fr.esgi.color_run.servlet;

import java.io.*;

import fr.esgi.color_run.business.Admin;
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
        System.out.println("[DEBUG] Token JWT récupéré dans /me : " + token);

        if (token == null) {
            // Si l'utilisateur n'est pas authentifié, renvoyer un code 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            Admin admin = null;
            Participant participant = authService.getParticipantFromToken(token);
            if (participant == null) {
                admin = authService.getAdminFromToken(token);
            }

            if (participant == null && admin == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Informations utilisateur introuvables\"}");
                return;
            }

            boolean isOrganisateur = Boolean.TRUE.equals(request.getAttribute("is_organisateur"));

            String userText;

            if (participant != null) {
                userText = "Utilisateur: " + participant.getNom() + " " + participant.getPrenom() +
                        "\nOrganisateur: " + isOrganisateur +
                        "\nAdmin: false" +
                        "\nDonnées utilisateur: " + participant.toString();

            } else {
                userText = "Utilisateur: " + admin.getNom() + " " + admin.getPrenom() +
                        "\nOrganisateur: false" +
                        "\nAdmin: true" +
                        "\nDonnées utilisateur: " + admin.toString();
            }

            response.getWriter().write(userText);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur s'est produite lors de la récupération des données utilisateur\"}");
        }
    }
}