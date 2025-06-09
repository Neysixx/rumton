package fr.esgi.color_run.servlet;

import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.VerificationService;
import fr.esgi.color_run.service.EmailService;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import fr.esgi.color_run.service.impl.VerificationServiceImpl;
import fr.esgi.color_run.service.impl.EmailServiceImpl;
import fr.esgi.color_run.business.Verification;
import fr.esgi.color_run.util.DebugUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import org.thymeleaf.context.Context;
import fr.esgi.color_run.business.Participant;

@WebServlet(name = "verificationServlet", value = {"/verify/*", "/verify", "/verify/resend"})
public class VerificationServlet extends BaseWebServlet {
    private ParticipantService participantService;
    private VerificationService verificationService;
    private EmailService emailService;

    @Override
    public void init() {
        super.init();
        participantService = new ParticipantServiceImpl();
        verificationService = new VerificationServiceImpl();
        emailService = new EmailServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        // Gestion du renvoi d'email
        if ("/resend".equals(pathInfo)) {
            handleResendEmail(request, response);
            return;
        }
        
        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Email parameter is required");
            return;
        }

        // Check if the email is valid and exists in the database
        // If it exists, check if the participant is verified
        // if not verified,  display the verification code input
        // else, redirect to the login page
        Context context = new Context();

        try {
            Participant participant = participantService.getParticipantByEmail(email);
            if (participant == null) {
                renderError(request, response, "Participant introuvable");
                return;
            }

            if (participant.isEstVerifie()){
                DebugUtil.log(this.getClass(), "Participant déjà vérifié : " + participant.getEmail());
                // Redirect to the login page
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                // Display the verification code input
                context.setVariable("email", email);
                renderTemplate(request, response, "auth/verification.html", context);
            }
        } catch (Exception e) {
            DebugUtil.log(this.getClass(), "Erreur lors de la vérification : " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Erreur interne du serveur");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        
        if ("/verify/resend".equals(servletPath) || "/resend".equals(pathInfo)) {
            handleResendEmail(request, response);
            return;
        }
        
        String email = request.getParameter("email");
        String verificationCode = request.getParameter("verification_code");

        if (email == null || email.trim().isEmpty() || verificationCode == null || verificationCode.trim().isEmpty()) {
            Context context = new Context();
            context.setVariable("email", email);
            context.setVariable("error", "Tous les champs sont obligatoires");
            renderTemplate(request, response, "auth/verification.html", context);
            return;
        }

        // Vérifier que le code contient exactement 6 chiffres
        if (!verificationCode.matches("\\d{6}")) {
            Context context = new Context();
            context.setVariable("email", email);
            context.setVariable("error", "Le code de vérification doit contenir exactement 6 chiffres");
            renderTemplate(request, response, "auth/verification.html", context);
            return;
        }

        // Check if the verification code is valid
        // If valid, update the participant's status to verified
        // else, display an error message
        Context context = new Context();

        try {
            Participant participant = participantService.getParticipantByEmail(email);
            if (participant == null) {
                context.setVariable("email", email);
                context.setVariable("error", "Participant introuvable");
                renderTemplate(request, response, "auth/verification.html", context);
                return;
            }

            if (verificationService.verifierCode(verificationCode, participant)) {
                DebugUtil.log(this.getClass(), "Code de vérification validé pour : " + participant.getEmail());
                participantService.verifierParticipant(participant);
                // Redirect to the login page with success message
                response.sendRedirect(request.getContextPath() + "/login?verified=true");
            } else {
                context.setVariable("email", email);
                context.setVariable("error", "Code de vérification invalide. Veuillez vérifier et réessayer.");
                renderTemplate(request, response, "auth/verification.html", context);
            }
        } catch (Exception e) {
            DebugUtil.log(this.getClass(), "Erreur lors de la vérification du code : " + e.getMessage());
            context.setVariable("email", email);
            context.setVariable("error", "Une erreur est survenue. Veuillez réessayer.");
            renderTemplate(request, response, "auth/verification.html", context);
        }
    }
    
    /**
     * Gère le renvoi d'email de vérification
     */
    private void handleResendEmail(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        
        if (email == null || email.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Email parameter is required");
            return;
        }

        try {
            Participant participant = participantService.getParticipantByEmail(email);
            if (participant == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("Participant introuvable");
                return;
            }

            if (participant.isEstVerifie()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Participant déjà vérifié");
                return;
            }

            // Générer un nouveau code de vérification
            String nouveauCode = emailService.genererCodeVerification();
            
            // Créer une nouvelle entrée de vérification avec le builder
            Verification verification = Verification.builder()
                    .participant(participant)
                    .code(nouveauCode)
                    .dateTime(new java.sql.Timestamp(System.currentTimeMillis()))
                    .dateTimeCompleted(new java.sql.Timestamp(System.currentTimeMillis() + 3600000)) // 1 heure
                    .build();
            verificationService.creerVerification(verification);

            // Envoyer l'email de vérification
            emailService.envoyerEmailVerification(email, nouveauCode);
            
            DebugUtil.log(this.getClass(), "Nouveau code de vérification envoyé à : " + email);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Email de vérification renvoyé avec succès");
            
        } catch (Exception e) {
            DebugUtil.log(this.getClass(), "Erreur lors du renvoi d'email : " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Erreur lors de l'envoi de l'email");
        }
    }
}
