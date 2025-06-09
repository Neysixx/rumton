package fr.esgi.color_run.util;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * Classe utilitaire pour la génération et vérification des JWT (JSON Web
 * Tokens)
 * Implémentation en Singleton pour éviter de créer plusieurs instances
 */
public class JwtUtil {
    private static final String SECRET_KEY_STRING = "ColorRunSecretKeyForJWTSigningMustBeLongEnoughForSecurity";
    private final Key secretKey;
    private final long expiration;

    // Instance unique du Singleton
    private static JwtUtil instance;

    /**
     * Constructeur privé pour empêcher l'instanciation directe
     */
    private JwtUtil() {
        this.secretKey = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());
        this.expiration = 24 * 60 * 60 * 1000; // 24 heures
    }

    /**
     * Méthode pour obtenir l'instance unique de JwtUtil
     * 
     * @return L'instance unique de JwtUtil
     */
    public static synchronized JwtUtil getInstance() {
        if (instance == null) {
            instance = new JwtUtil();
        }
        return instance;
    }

    /**
     * Génère un token JWT pour l'utilisateur spécifié
     * 
     * @param user L'utilisateur (Participant ou Admin)
     * @return Le token JWT généré
     * @throws IllegalArgumentException si l'utilisateur n'est ni un Admin ni un
     *                                  Participant
     */
    public String generateToken(Object user) {
        Date now = new Date();

        String subject;
        int userId;
        String role;
        String email;
        boolean isVerified;

        if (user instanceof Participant) {
            Participant participant = (Participant) user;
            subject = participant.getEmail();
            userId = participant.getIdParticipant();
            role = participant.isEstOrganisateur() ? "ORGANISATEUR" : "PARTICIPANT";
            isVerified = participant.isEstVerifie();
            email = participant.getEmail();
        } else if (user instanceof Admin) {
            Admin admin = (Admin) user;
            subject = admin.getEmail();
            userId = admin.getIdAdmin();
            role = "ADMIN";
            isVerified = true;
            email = admin.getEmail();
        } else {
            throw new IllegalArgumentException("L'utilisateur doit être un Admin ou un Participant");
        }

        return Jwts.builder()
                .setSubject(subject)
                .claim("userId", userId)
                .claim("role", role)
                .claim("is_verified", isVerified)
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + this.expiration))
                .signWith(this.secretKey)
                .compact();
    }

    /**
     * Parse et vérifie un token JWT
     * 
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
     * 
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
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException
                | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extrait l'ID utilisateur du token
     * 
     * @param token Le token JWT
     * @return L'ID de l'utilisateur
     */
    public int getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Integer.class);
    }

    /**
     * Extrait le rôle de l'utilisateur du token
     * 
     * @param token Le token JWT
     * @return Le rôle de l'utilisateur (ADMIN, PARTICIPANT, ORGANISATEUR)
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Extrait l'email (subject) du token
     * 
     * @param token Le token JWT
     * @return L'email de l'utilisateur
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * Vérifie si le token appartient à un participant vérifié
     * 
     * @param token Le token JWT
     * @return true si le token appartient à un participant vérifié, false sinon
     */
    public boolean isVerified(String token) {
        Claims claims = parseToken(token);
        return claims.get("is_verified", Boolean.class);
    }
}