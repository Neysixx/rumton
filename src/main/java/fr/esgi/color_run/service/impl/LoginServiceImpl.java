package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.repository.AdminRepository;
import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.service.LoginService;
import fr.esgi.color_run.util.CryptUtil;
import fr.esgi.color_run.util.JwtUtil;

import java.util.Optional;

public class LoginServiceImpl implements LoginService {
    
    private final ParticipantRepository participantRepository;
    private final AdminRepository adminRepository;
    private final JwtUtil jwtUtil;

    public LoginServiceImpl(ParticipantRepository participantRepository, AdminRepository adminRepository, JwtUtil jwtUtil) {
        this.participantRepository = participantRepository;
        this.adminRepository = adminRepository;
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
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);

        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (CryptUtil.checkPassword(password, admin.getMotDePasse())) {
                return jwtUtil.generateToken(admin);
            }
        }

        return null;
    }
}