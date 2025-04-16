package fr.esgi.color_run.util;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * Classe utilitaire pour la génération et vérification des JWT (JSON Web Tokens)
 */
public class JwtUtil {
    
    private final Key secretKey;
    private final long expiration;
    
    /**
     * Constructeur par défaut avec clé générée aléatoirement et expiration de 24 heures
     */
    public JwtUtil() {
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        this.expiration = 24 * 60 * 60 * 1000; // 24 heures
    }
    
    /**
     * Constructeur avec clé et durée d'expiration personnalisées
     */
    public JwtUtil(Key secretKey, long expiration) {
        this.secretKey = secretKey;
        this.expiration = expiration;
    }

    /**
     * Génère un token JWT pour l'utilisateur spécifié
     * @param user L'utilisateur (Participant ou Admin)
     * @return Le token JWT généré
     * @throws IllegalArgumentException si l'utilisateur n'est ni un Admin ni un Participant
     */
    public String generateToken(Object user) {
        Date now = new Date();

        String subject;
        int userId;
        String role;

        if (user instanceof Participant) {
            Participant participant = (Participant) user;
            subject = participant.getEmail();
            userId = participant.getIdParticipant();
            role = participant.isEstOrganisateur() ? "ORGANISATEUR" : "PARTICIPANT";
        } else if (user instanceof Admin) {
            Admin admin = (Admin) user;
            subject = admin.getEmail();
            userId = admin.getIdAdmin();
            role = "ADMIN";
        } else {
            throw new IllegalArgumentException("L'utilisateur doit être un Admin ou un Participant");
        }

        return Jwts.builder()
                .setSubject(subject)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + this.expiration))
                .signWith(this.secretKey)
                .compact();
    }
    
    /**
     * Parse et vérifie un token JWT
     * @param token Le token JWT à vérifier
     * @return Les claims du token si valide
     * @throws Exception si le token est invalide
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Valide un token JWT
     * @param token Le token JWT à valider
     * @return true si le token est valide, false sinon
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | 
                UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Extrait l'ID utilisateur du token
     * @param token Le token JWT
     * @return L'ID de l'utilisateur
     */
    public int getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Integer.class);
    }
    
    /**
     * Extrait le rôle de l'utilisateur du token
     * @param token Le token JWT
     * @return Le rôle de l'utilisateur (ADMIN, PARTICIPANT, ORGANISATEUR)
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }
    
    /**
     * Extrait l'email (subject) du token
     * @param token Le token JWT
     * @return L'email de l'utilisateur
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
}
