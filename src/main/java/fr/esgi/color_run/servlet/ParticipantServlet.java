package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import fr.esgi.color_run.util.CryptUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Servlet pour gérer les participants
 */
@WebServlet(name = "participantServlet", value = {"/participants", "/participants/*"})
public class ParticipantServlet extends BaseWebServlet {

    private AuthService authService;
    private ParticipantService participantService;

    @Override
    public void init() {
        super.init();
        authService = new AuthServiceImpl();
        participantService = new ParticipantServiceImpl();
    }

    /**
     * Traite les requêtes GET pour afficher les participants
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            renderError(request, response, "Utilisateur non authentifié");
            return;
        }

        // Vérification que l'utilisateur est admin (seul un admin peut lister les participants)
        boolean isAdmin = isAdmin(request, response);
        if (!isAdmin) {
            renderError(request, response, "Accès non autorisé");
            return;
        }

        Context context = new Context();

        // Analyse du chemin pour déterminer le type de requête
        String pathInfo = request.getPathInfo();

        try {
            // URL pattern: /participants/{id}
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                int participantId = Integer.parseInt(pathInfo.substring(1));
                
                // Récupération du participant
                Participant participant = participantService.getParticipantById(participantId);
                if (participant == null) {
                    renderError(request, response, "Participant non trouvé");
                    return;
                }
                
                // On ne renvoie pas le mot de passe
                participant.setMotDePasse(null);
                
                // Ajout à la vue
                context.setVariable("user", participant);

                renderTemplate(request, response, "admin/editUser", context);
            }
            // URL pattern: /participants
            else {
                // Récupération de tous les participants
                List<Participant> participants = participantService.getAllParticipants();
                
                // Masquer les mots de passe
                participants.forEach(p -> p.setMotDePasse(null));
                
                // Ajout à la vue
                context.setVariable("participants", participants);

                renderTemplate(request, response, "admin/users", context);
            }
            
        } catch (NumberFormatException e) { 
            renderError(request, response, "Format d'ID invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Traite les requêtes POST pour mettre à jour un participant
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            renderError(request, response, "Utilisateur non authentifié");
            return;
        }
        
        try {
            // Vérification du chemin
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new IllegalArgumentException("ID de participant invalide ou manquant");
            }
            
            int participantId = Integer.parseInt(pathInfo.substring(1));
            
            // Récupération du participant
            Participant participant = participantService.getParticipantById(participantId);
            if (participant == null) {
                renderError(request, response, "Participant non trouvé");
                return;
            }
            
            // Vérification des autorisations
            Participant currentUser = getAuthenticatedParticipant(request);
            boolean isAdmin = isAdmin(request, response);
            
            // Seul l'admin ou le participant lui-même peut modifier ses informations
            if (!isAdmin && (currentUser == null || currentUser.getIdParticipant() != participantId)) {
                renderError(request, response, "Vous n'êtes pas autorisé à modifier ce participant");
                return;
            }
            
            // Mise à jour des champs
            String nom = request.getParameter("nom");
            if (nom != null && !nom.trim().isEmpty()) {
                participant.setNom(nom);
            }
            
            String prenom = request.getParameter("prenom");
            if (prenom != null && !prenom.trim().isEmpty()) {
                participant.setPrenom(prenom);
            }
            
            String email = request.getParameter("email");
            if (email != null && !email.trim().isEmpty() && !email.equals(participant.getEmail())) {
                // Vérifier que l'email n'est pas déjà utilisé
                if (participantService.existsByEmail(email)) {
                    renderError(request, response, "Cette adresse email est déjà utilisée");
                    return;
                }
                participant.setEmail(email);
            }
            
            // Seul l'admin peut modifier ces champs
            if (isAdmin) {
                String role = request.getParameter("role");
                if (role != null) {
                    boolean estOrganisateur = role.equals("organisateur");
                    participant.setEstOrganisateur(estOrganisateur);
                }
            }
            
            // Enregistrement des modifications
            participantService.updateParticipant(participant);
            
            // Réponse
            response.sendRedirect(request.getContextPath() + "/participants");
            
        } catch (NumberFormatException e) {
            renderError(request, response, "Format d'ID invalide");
        } catch (IllegalArgumentException e) {
            renderError(request, response, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Traite les requêtes DELETE pour supprimer un participant
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        String token = (String) request.getAttribute("jwt_token");
        if (!isAuthenticated(request, response)) {
            renderError(request, response, "Droits d'administrateur requis");
            return;
        }

        boolean isAdmin = isAdmin(request, response);
        if (!isAdmin) {
            renderError(request, response, "Droits d'administrateur requis");
            return;
        }
        
        try {
            // Récupération de l'ID du participant
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new IllegalArgumentException("ID de participant invalide ou manquant");
            }
            
            int participantId = Integer.parseInt(pathInfo.substring(1));
            
            // Vérification que le participant existe
            Participant participant = participantService.getParticipantById(participantId);
            if (participant == null) {
                renderError(request, response, "Participant non trouvé");
                return;
            }
            
            // Suppression du participant
            boolean supprime = participantService.deleteParticipant(participantId);
            
            if (!supprime) {
                renderError(request, response, "Échec de la suppression du participant");
            }
            
        } catch (NumberFormatException e) {
            renderError(request, response, "Format d'ID invalide");
        } catch (IllegalArgumentException e) {
            renderError(request, response, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }
}
