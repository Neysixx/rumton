package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.DemandeOrganisateur;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.enums.EDemandeStatus;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.DemandeOrganisateurService;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.service.impl.DemandeOrganisateurServiceImpl;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Servlet pour gérer les demandes pour devenir organisateur
 */
@WebServlet(name = "demandeOrganisateurServlet", value = {"/demandes", "/demandes/*"})
public class DemandeOrganisateurServlet extends BaseWebServlet {

    private DemandeOrganisateurService demandeService;
    private ParticipantService participantService;

    @Override
    public void init() {
        demandeService = new DemandeOrganisateurServiceImpl();
        participantService = new ParticipantServiceImpl();
    }

    /**
     * Traite les requêtes GET pour afficher les demandes
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // On récupère le moteur de template dans le contexte des servlets
        Context context = new Context();

        // Analyse du chemin pour déterminer le type de requête
        String pathInfo = request.getPathInfo();
        try {
            // URL pattern: /demandes/{id}
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                int demandeId = Integer.parseInt(pathInfo.substring(1));
                
                // Récupération de la demande
                Optional<DemandeOrganisateur> optDemande = demandeService.getDemandeById(demandeId);
                if (optDemande.isEmpty()) {
                    renderError(request, response, "Demande non trouvée");
                    return;
                }
                
                DemandeOrganisateur demande = optDemande.get();
                
                // Vérification des permissions (admin ou participant concerné)
                boolean isAdmin = isAdmin(request, response);
                Participant currentUser = getAuthenticatedParticipant(request);
                
                if (!isAdmin && (currentUser == null || currentUser.getIdParticipant() != demande.getParticipant().getIdParticipant())) {
                    renderError(request, response, "Vous n'êtes pas autorisé à consulter cette demande");
                    return;
                }
                
                // Ajout à la vue
                context.setVariable("demande", demande);
                
                renderTemplate(request, response, "demande-organisateur", context);
            }
            // URL pattern: /demandes
            else {
                boolean isAdmin = isAdmin(request, response);
                Participant currentUser = getAuthenticatedParticipant(request);
                
                List<DemandeOrganisateur> demandes;
                
                // Si admin, afficher toutes les demandes
                if (isAdmin) {
                    // Filtrage optionnel par statut
                    String status = request.getParameter("status");
                    if (status != null && !status.trim().isEmpty()) {
                        demandes = demandeService.getDemandesByStatus(status);
                    } else {
                        demandes = demandeService.getAllDemandes();
                    }
                }
                // Sinon, n'afficher que les demandes du participant connecté
                else if (currentUser != null) {
                    demandes = demandeService.getDemandesByParticipant(currentUser.getIdParticipant());
                } else {
                    renderError(request, response, "Accès refusé");
                    return;
                }
                
                // Ajout à la vue
                context.setVariable("demandes", demandes);
                context.setVariable("isAdmin", isAdmin);
                
                renderTemplate(request, response, "demandes-organisateur", context);
            }
            
        } catch (NumberFormatException e) {
            renderError(request, response, "Format d'ID invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Traite les requêtes POST pour créer une nouvelle demande
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            // Récupération de l'utilisateur courant
            Participant participant = getAuthenticatedParticipant(request);
            if (participant == null) {
                throw new ServletException("Impossible de récupérer les informations du participant");
            }
            
            // Vérification que le participant n'est pas déjà organisateur
            if (participant.isEstOrganisateur()) {
                renderError(request, response, "Vous êtes déjà organisateur");
                return;
            }
            
            // Vérification qu'une demande en cours n'existe pas déjà
            if (demandeService.hasDemandeEnCours(participant.getIdParticipant())) {
                renderError(request, response, "Vous avez déjà une demande en cours");
                return;
            }
            
            // Récupération des motivations
            String motivations = request.getParameter("motivations");
            if (motivations == null || motivations.trim().isEmpty()) {
                throw new IllegalArgumentException("Les motivations sont obligatoires");
            }
            
            // Création de la demande
            DemandeOrganisateur demande = DemandeOrganisateur.builder()
                    .participant(participant)
                    .motivations(motivations)
                    .status(EDemandeStatus.EN_ATTENTE.toString())
                    .dateCreation(new Date())
                    .build();
            
            // Enregistrement de la demande
            demandeService.createDemande(demande);
            
            // Réponse
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"message\": \"Demande enregistrée avec succès\"}");
            
        } catch (IllegalArgumentException e) {
            renderError(request, response, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Traite les requêtes PUT pour traiter une demande (accepter/refuser)
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        if (!isAuthenticated(request, response)) {
            return;
        }

        boolean isAdmin = isAdmin(request, response);
        if (!isAdmin) {
            renderError(request, response, "Droits d'administrateur requis");
            return;
        }
        
        try {
            // Récupération de l'ID de la demande
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new IllegalArgumentException("ID de demande invalide ou manquant");
            }
            
            int demandeId = Integer.parseInt(pathInfo.substring(1));
            
            // Récupération de la demande
            Optional<DemandeOrganisateur> optDemande = demandeService.getDemandeById(demandeId);
            if (optDemande.isEmpty()) {
                renderError(request, response, "Demande non trouvée");
                return;
            }
            
            DemandeOrganisateur demande = optDemande.get();
            
            // Vérification que la demande n'a pas déjà été traitée
            if (!EDemandeStatus.EN_ATTENTE.toString().equals(demande.getStatus())) {
                renderError(request, response, "Cette demande a déjà été traitée");
                return;
            }
            
            // Récupération de la décision (acceptation ou refus)
            String decisionStr = request.getParameter("decision");
            if (decisionStr == null || decisionStr.trim().isEmpty()) {
                throw new IllegalArgumentException("La décision est obligatoire");
            }
            
            boolean decision = Boolean.parseBoolean(decisionStr);
            
            // Récupération de l'admin qui traite la demande
            Admin admin = getAuthenticatedAdmin(request);
            if (admin == null) {
                throw new ServletException("Impossible de récupérer les informations de l'administrateur");
            }
            
            // Mise à jour de la demande
            demande.setStatus(decision ? EDemandeStatus.ACCEPTEE.toString() : EDemandeStatus.REFUSEE.toString());
            demande.setReponse(decision);
            demande.setTraitePar(Optional.of(admin));
            demande.setDateTraitement(Optional.of(new Date()));
            
            // Enregistrement des modifications
            demandeService.updateDemande(demande);
            
            // Si la demande est acceptée, donner le statut d'organisateur au participant
            if (decision) {
                Participant participant = demande.getParticipant();
                participant.setEstOrganisateur(true);
                participantService.updateParticipant(participant);
            }
            
            // Réponse
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"message\": \"Demande " + (decision ? "acceptée" : "refusée") + " avec succès\"}");
            
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
