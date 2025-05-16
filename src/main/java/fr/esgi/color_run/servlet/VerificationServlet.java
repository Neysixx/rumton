package fr.esgi.color_run.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.repository.impl.ParticipantRepositoryImpl;

@WebServlet(name = "causeServlet", value = {"/verify/*", "/verify"})
public class VerificationServlet extends BaseWebServlet {

    private ParticipantRepositoryImpl participantRepository;

    @Override
    public void init() {
        participantRepository = new ParticipantRepositoryImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        if (email == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Optional<Participant> participant = participantRepository.findByEmail(email);
            if (participant.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (participant.get().isEmailVerified()) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }

            participant.get().setEmailVerified(true);
            participantRepository.save(participant.get());

            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
