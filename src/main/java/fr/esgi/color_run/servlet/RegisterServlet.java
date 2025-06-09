package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.DemandeOrganisateur;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.enums.EDemandeStatus;
import fr.esgi.color_run.service.DemandeOrganisateurService;
import fr.esgi.color_run.business.Verification;
import fr.esgi.color_run.service.EmailService;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.DemandeOrganisateurServiceImpl;
import fr.esgi.color_run.service.VerificationService;
import fr.esgi.color_run.service.impl.EmailServiceImpl;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import fr.esgi.color_run.service.impl.VerificationServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static fr.esgi.color_run.util.CryptUtil.hashPassword;

/**
 * Servlet qui gère l'inscription des nouveaux participants
 */
@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends BaseWebServlet {

    private ParticipantService participantService;
    private EmailService emailService;
    private VerificationService verificationService;
    private DemandeOrganisateurService demandeOrganisateurService;

    @Override
    public void init() {
        super.init();
        participantService = new ParticipantServiceImpl();
        demandeOrganisateurService = new DemandeOrganisateurServiceImpl();
        emailService = new EmailServiceImpl();
        verificationService = new VerificationServiceImpl();
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
        String role = request.getParameter("role");
        String motivations = request.getParameter("motivations");
        Boolean isOrganisateurRequest = false;

        Context context = new Context();

        // Validation des données
        if (nom == null || prenom == null || email == null || motDePasse == null || nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || motDePasse.isEmpty()) {
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

        // Vérification du role
        if(role == null || role.isEmpty()) {
            context.setVariable("error", "Vous devez selectionner un role");
            renderTemplate(request, response, "auth/register", context);
        }else{
            // Traitement du role organisateur
            if(role.equalsIgnoreCase("organisateur")){
                if(motivations == null || motivations.isEmpty()){
                    context.setVariable("error", "Vous devez indiquer vos motivations pour devenir organisateur");
                    renderTemplate(request, response, "auth/register", context);
                } else{
                    isOrganisateurRequest = true;
                }
            }
        }

        // Création du participant
        Participant participant = Participant.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .motDePasse(hashPassword(motDePasse))
                .estOrganisateur(false)
                .dateCreation(new Date())
                .estVerifie(false)
                .build();

        String code = emailService.genererCodeVerification();

        // Création de l'objet Verification expirant dans 1 heure
        Verification verification = Verification.builder()
                .participant(participant)
                .code(code)
                .dateTime(Timestamp.from(Instant.now()))
                .dateTimeCompleted(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .build();

        // Enregistrement du participant et envoi de l'email de vérification
        try {
            emailService.envoyerEmailVerification(email, code);
            participantService.creerParticipant(participant);
            verificationService.creerVerification(verification);

            // si le participant veut devenir organisateur
            if(isOrganisateurRequest){
                DemandeOrganisateur demande = DemandeOrganisateur.builder()
                        .participant(participant)
                        .motivations(motivations)
                        .status(EDemandeStatus.EN_ATTENTE.toString())
                        .dateCreation(new Date())
                        .build();

                // Enregistrement de la demande
                demandeOrganisateurService.createDemande(demande);
            }

            // Redirection vers la page de connexion avec un message de succès
            response.sendRedirect(request.getContextPath() + "/login?registered=true");
        } catch (Exception e) {
            context.setVariable("error", "Une erreur est survenue lors de l'inscription: " + e.getMessage());
            renderTemplate(request, response, "auth/register", context);
        }
    }
}
