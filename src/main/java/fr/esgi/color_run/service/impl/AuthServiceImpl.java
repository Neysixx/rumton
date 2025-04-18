package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.repository.AdminRepository;
import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.repository.impl.AdminRepositoryImpl;
import fr.esgi.color_run.repository.impl.ParticipantRepositoryImpl;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.util.JwtUtil;

public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final ParticipantRepository participantRepository;
    private final AdminRepository adminRepository;

    public AuthServiceImpl() {
        this.jwtUtil = JwtUtil.getInstance();
        this.participantRepository = new ParticipantRepositoryImpl();
        this.adminRepository = new AdminRepositoryImpl();
    }

    public AuthServiceImpl(JwtUtil jwtUtil, ParticipantRepository participantRepository, AdminRepository adminRepository) {
        this.jwtUtil = jwtUtil;
        this.participantRepository = participantRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return jwtUtil.validateToken(token);
    }

    @Override
    public int getUserIdFromToken(String token) {
        if (!isTokenValid(token)) {
            return -1;
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    @Override
    public Participant getParticipantFromToken(String token) {
        if (!isTokenValid(token)) {
            System.out.println("Token invalide");
            return null;
        }
        
        String role = jwtUtil.getRoleFromToken(token);
        if (!"PARTICIPANT".equals(role) && !"ORGANISATEUR".equals(role)) {
            return null;
        }
        
        int userId = jwtUtil.getUserIdFromToken(token);
        return participantRepository.findById(userId).orElse(null);
    }

    @Override
    public Admin getAdminFromToken(String token) {
        if (!isTokenValid(token)) {
            return null;
        }
        
        String role = jwtUtil.getRoleFromToken(token);
        if (!"ADMIN".equals(role)) {
            return null;
        }
        
        int userId = jwtUtil.getUserIdFromToken(token);
        return adminRepository.findById(userId).orElse(null);
    }

    @Override
    public boolean isAdmin(String token) {
        if (!isTokenValid(token)) {
            return false;
        }
        
        String role = jwtUtil.getRoleFromToken(token);
        return "ADMIN".equals(role);
    }

    @Override
    public boolean isOrganisateur(String token) {
        if (!isTokenValid(token)) {
            return false;
        }
        
        String role = jwtUtil.getRoleFromToken(token);
        return "ORGANISATEUR".equals(role);
    }

    @Override
    public String extractToken(String authHeader, String cookieToken) {
        // Vérifier d'abord le header Authorization
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Sinon retourner le token du cookie
        return cookieToken;
    }
}
