package fr.esgi.color_run.servlet;

import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.VerificationService;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import fr.esgi.color_run.service.impl.VerificationServiceImpl;
import fr.esgi.color_run.util.DebugUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.thymeleaf.context.Context;
import fr.esgi.color_run.business.Participant;

@WebServlet(name = "verificationServlet", value = {"/verify/*", "/verify"})
public class VerificationServlet extends BaseWebServlet {
    private ParticipantService participantService;
    private VerificationService verificationService;

    @Override
    public void init() {
        super.init();
        participantService = new ParticipantServiceImpl();
        verificationService = new VerificationServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        if (email == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
                renderError(request, response, "Participant not found");
                return;
            }

            if (participant.isEstVerifie()){
                DebugUtil.log("Participant is verified : " + participant.getEmail());
                // Redirect to the login page
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                // Display the verification code input
                context.setVariable("email", email);
                renderTemplate(request, response, "auth/verification.html", context);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String verificationCode = request.getParameter("verification_code");

        if (email == null || verificationCode == null) {
            renderError(request, response, "Missing parameters");
            return;
        }

        // Check if the verification code is valid
        // If valid, update the participant's status to verified
        // else, display an error message
        Context context = new Context();

        try {
            Participant participant = participantService.getParticipantByEmail(email);
            if (participant == null) {
                renderError(request, response, "Participant not found");
                return;
            }

            if (verificationService.verifierCode(verificationCode, participant)) {
                // Redirect to the login page
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                context.setVariable("error", "Invalid verification code");
                renderTemplate(request, response, "verification.html", context);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
