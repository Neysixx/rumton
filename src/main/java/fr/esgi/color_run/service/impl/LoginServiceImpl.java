package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.service.LoginService;
import fr.esgi.color_run.util.CryptUtil;
import fr.esgi.color_run.util.JwtUtil;

import java.util.Optional;

public class LoginServiceImpl implements LoginService {
    
    private final ParticipantRepository participantRepository;
    private final JwtUtil jwtUtil;

    public LoginServiceImpl(ParticipantRepository participantRepository, JwtUtil jwtUtil) {
        this.participantRepository = participantRepository;
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    public String authenticateParticipant(String email, String password) {
        Optional<Participant> participantOpt = participantRepository.findByEmail(email);

        if (participantOpt.isPresent()) {
            Participant participant = participantOpt.get();
            if (CryptUtil.checkPassword(password, participant.getMotDePasse())) {
                String token = this.jwtUtil.generateToken(participant);
                return token;
            }
        }
        
        return null;
    }
    
    @Override
    public String authenticateAdmin(String email, String password) {
        // Cette méthode sera implémentée plus tard lorsque la classe Admin
        // et son repository seront disponibles
        return null;
    }
}
