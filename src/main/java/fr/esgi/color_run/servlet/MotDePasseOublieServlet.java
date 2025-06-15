package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Verification;
import fr.esgi.color_run.service.EmailService;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.VerificationService;
import fr.esgi.color_run.service.impl.EmailServiceImpl;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import fr.esgi.color_run.service.impl.VerificationServiceImpl;
import fr.esgi.color_run.util.CryptUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.Timestamp;

@WebServlet(name = "motDePasseOublieServlet", value = "/mot-de-passe-oublie")
public class MotDePasseOublieServlet extends BaseWebServlet {
    
    private ParticipantService participantService;
    private EmailService emailService;
    private VerificationService verificationService;
    
    @Override
    public void init() {
        super.init();
        participantService = new ParticipantServiceImpl();
        emailService = new EmailServiceImpl();
        verificationService = new VerificationServiceImpl();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Context context = new Context();
        
        // Récupérer les paramètres de la requête
        String step = request.getParameter("step");
        String email = request.getParameter("email");
        
        if (step == null || step.equals("1")) {
            // Étape 1: Afficher le formulaire de saisie d'email
            context.setVariable("step", "1");
            context.setVariable("titre", "Mot de passe oublié");
            context.setVariable("description", "Saisissez votre adresse email pour recevoir un code de réinitialisation");
        } else if (step.equals("2") && email != null) {
            // Étape 2: Afficher le formulaire de saisie du code + nouveau mot de passe
            context.setVariable("step", "2");
            context.setVariable("email", email);
            context.setVariable("titre", "Code de vérification");
            context.setVariable("description", "Saisissez le code reçu par email et votre nouveau mot de passe");
        } else {
            // Redirection vers l'étape 1 en cas d'erreur
            response.sendRedirect(request.getContextPath() + "/mot-de-passe-oublie?step=1");
            return;
        }
        
        renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String step = request.getParameter("step");
        Context context = new Context();
        
        if ("1".equals(step)) {
            // Étape 1: Traitement de l'email
            handleEmailSubmission(request, context, response);
        } else if ("2".equals(step)) {
            // Étape 2: Traitement du code et nouveau mot de passe
            handlePasswordReset(request, context, response);
        } else {
            context.setVariable("error", "Étape invalide");
            context.setVariable("step", "1");
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
        }
    }
    
    private void handleEmailSubmission(HttpServletRequest request, Context context, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        
        if (email == null || email.trim().isEmpty()) {
            context.setVariable("error", "L'adresse email est obligatoire");
            context.setVariable("step", "1");
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
            return;
        }
        
        // Vérifier si l'email existe dans la base de données
        Participant participant = participantService.getParticipantByEmail(email.trim());
        
        if (participant == null) {
            context.setVariable("error", "Aucun compte n'est associé à cette adresse email");
            context.setVariable("step", "1");
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
            return;
        }
        
        try {
            // Générer un code de vérification
            String code = emailService.genererCodeVerification();
            
            Timestamp now = new Timestamp(System.currentTimeMillis());

            // Créer une entrée de vérification
            Verification verification = Verification.builder()
                    .participant(participant)
                    .code(code)
                    .dateTime(now)
                    .dateTimeCompleted(new Timestamp(now.getTime() + 15 * 60 * 1000))
                    .build();
            
            verificationService.creerVerification(verification);
            
            // Envoyer l'email avec le code
            String sujet = "Réinitialisation de votre mot de passe - Color Run";
            String contenu = "<html><body>" +
                    "<h2>Réinitialisation de mot de passe</h2>" +
                    "<p>Bonjour " + participant.getPrenom() + ",</p>" +
                    "<p>Vous avez demandé la réinitialisation de votre mot de passe.</p>" +
                    "<p><strong>Votre code de vérification est : " + code + "</strong></p>" +
                    "<p>Ce code est valable pendant 15 minutes.</p>" +
                    "<p>Si vous n'avez pas demandé cette réinitialisation, ignorez ce message.</p>" +
                    "<br><p>L'équipe Color Run</p>" +
                    "</body></html>";
            
            emailService.envoyerEmail(email, sujet, contenu);
            
            // Rediriger vers l'étape 2
            response.sendRedirect(request.getContextPath() + "/mot-de-passe-oublie?step=2&email=" + email);
            
        } catch (MessagingException e) {
            context.setVariable("error", "Erreur lors de l'envoi de l'email. Veuillez réessayer.");
            context.setVariable("step", "1");
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
        } catch (Exception e) {
            context.setVariable("error", "Une erreur technique est survenue. Veuillez réessayer.");
            context.setVariable("step", "1");
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
        }
    }
    
    private void handlePasswordReset(HttpServletRequest request, Context context, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String code = request.getParameter("code");
        String nouveauMotDePasse = request.getParameter("nouveauMotDePasse");
        String confirmationMotDePasse = request.getParameter("confirmationMotDePasse");
        
        // Validation des champs
        if (email == null || code == null || nouveauMotDePasse == null || confirmationMotDePasse == null ||
            email.trim().isEmpty() || code.trim().isEmpty() || nouveauMotDePasse.trim().isEmpty()) {
            context.setVariable("error", "Tous les champs sont obligatoires");
            context.setVariable("step", "2");
            context.setVariable("email", email);
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
            return;
        }
        
        // Vérifier que les mots de passe correspondent
        if (!nouveauMotDePasse.equals(confirmationMotDePasse)) {
            context.setVariable("error", "Les mots de passe ne correspondent pas");
            context.setVariable("step", "2");
            context.setVariable("email", email);
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
            return;
        }
        
        // Vérifier la longueur du mot de passe
        if (nouveauMotDePasse.length() < 6) {
            context.setVariable("error", "Le mot de passe doit contenir au moins 6 caractères");
            context.setVariable("step", "2");
            context.setVariable("email", email);
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
            return;
        }
        
        // Récupérer le participant
        Participant participant = participantService.getParticipantByEmail(email.trim());
        
        if (participant == null) {
            context.setVariable("error", "Email invalide");
            context.setVariable("step", "1");
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
            return;
        }
        
        // Vérifier le code de vérification
        boolean codeValide = verificationService.verifierCode(code.trim(), participant);
        
        if (!codeValide) {
            context.setVariable("error", "Code de vérification invalide ou expiré");
            context.setVariable("step", "2");
            context.setVariable("email", email);
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
            return;
        }
        
        try {
            // Hasher le nouveau mot de passe
            String motDePasseHashe = CryptUtil.hashPassword(nouveauMotDePasse);
            
            // Mettre à jour le mot de passe du participant
            participant.setMotDePasse(motDePasseHashe);
            participantService.updateParticipant(participant);
            
            // Succès : rediriger vers la page de connexion avec un message
            response.sendRedirect(request.getContextPath() + "/login?success=password_reset");
            
        } catch (Exception e) {
            context.setVariable("error", "Une erreur est survenue lors de la mise à jour du mot de passe");
            context.setVariable("step", "2");
            context.setVariable("email", email);
            renderTemplate(request, response, "auth/mot-de-passe-oublie", context);
        }
    }
}
