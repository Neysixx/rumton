package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Date;

import static fr.esgi.color_run.util.CryptUtil.hashPassword;

/**
 * Servlet qui gère l'inscription des nouveaux participants
 */
@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends BaseWebServlet {

    private ParticipantService participantService;

    @Override
    public void init() {
        super.init();
        participantService = new ParticipantServiceImpl();
    }

    /**
     * Affiche le formulaire d'inscription
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Création du contexte Thymeleaf
        Context context = new Context();

        // Traitement de la page
        renderTemplate(request, response, "auth/register", context);
    }

    /**
     * Traite les données du formulaire d'inscription
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Récupération des paramètres du formulaire
        String nom = request.getParameter("nom");
        String prenom = request.getParameter("prenom");
        String email = request.getParameter("email");
        String motDePasse = request.getParameter("motDePasse");
        String confirmMotDePasse = request.getParameter("confirmMotDePasse");

        // Debug des données reçues
        System.out.println("DEBUG - Données reçues:");
        System.out.println("Nom: " + nom);
        System.out.println("Prénom: " + prenom);
        System.out.println("Email: " + email);
        System.out.println("Mot de passe reçu: " + (motDePasse != null ? "Oui (longueur: " + motDePasse.length() + ")" : "Non"));

        Context context = new Context();

        // Validation des données
        if (nom == null || prenom == null || email == null || motDePasse == null ||
                nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || motDePasse.isEmpty()) {

            context.setVariable("error", "Tous les champs sont obligatoires");
            renderTemplate(request, response, "auth/register", context);
            return;
        }

        // Vérification que les mots de passe correspondent
        if (!motDePasse.equals(confirmMotDePasse)) {
            context.setVariable("error", "Les mots de passe ne correspondent pas");
            renderTemplate(request, response, "auth/register", context);
            return;
        }

        // Vérification si l'email est déjà utilisé
        if (participantService.existsByEmail(email)) {
            context.setVariable("error", "Cette adresse email est déjà utilisée");
            renderTemplate(request, response, "auth/register", context);
            return;
        }

        // Création du participant
        Participant participant = Participant.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .motDePasse(hashPassword(motDePasse))
                .estOrganisateur(false)
                .dateCreation(new Date())
                .build();

        System.out.println("DEBUG - Participant créé: " + participant);

        // Enregistrement du participant
        try {
            participantService.creerParticipant(participant);

            // Redirection vers la page de connexion avec un message de succès
            response.sendRedirect(request.getContextPath() + "login?registered=true");
        } catch (Exception e) {
            context.setVariable("error", "Une erreur est survenue lors de l'inscription: " + e.getMessage());
            renderTemplate(request, response, "auth/register", context);
        }
    }
}
