package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import fr.esgi.color_run.util.CryptUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Servlet pour gérer les profils utilisateurs
 */
@WebServlet(name = "profileServlet", value = {"/profile", "/profile/*"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,     // 1 MB
    maxFileSize = 1024 * 1024 * 10,      // 10 MB
    maxRequestSize = 1024 * 1024 * 15    // 15 MB
)
public class ProfileServlet extends HttpServlet {

    private AuthService authService;
    private ParticipantService participantService;
    private static final String UPLOAD_DIRECTORY = "uploads";

    @Override
    public void init() {
        authService = new AuthServiceImpl();
        participantService = new ParticipantServiceImpl();
        
        // Création du répertoire d'upload s'il n'existe pas
        String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();
    }

    /**
     * Traite les requêtes GET pour afficher le profil utilisateur
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        // On récupère le moteur de template dans le contexte des servlets
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        Context context = new Context();

        try {
            Participant currentUser = authService.getParticipantFromToken(token);
            if (currentUser == null) {
                throw new ServletException("Impossible de récupérer les informations de l'utilisateur");
            }
            
            String pathInfo = request.getPathInfo();
            
            // Si l'URL contient un ID utilisateur, afficher ce profil spécifique
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                int userId = Integer.parseInt(pathInfo.substring(1));
                
                // Vérifier si on est admin ou si on consulte son propre profil
                boolean isAdmin = authService.isAdmin(token);
                boolean isOwnProfile = currentUser.getIdParticipant() == userId;
                
                if (!isAdmin && !isOwnProfile) {
                    // Affichage du profil public (moins d'informations)
                    Participant publicUser = participantService.getParticipantById(userId);
                    if (publicUser == null) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\": \"Utilisateur non trouvé\"}");
                        return;
                    }
                    
                    // On cache certaines informations sensibles
                    publicUser.setMotDePasse(null);
                    
                    context.setVariable("participant", publicUser);
                    context.setVariable("isPublicView", true);
                } else {
                    // Affichage du profil complet
                    Participant user = participantService.getParticipantById(userId);
                    if (user == null) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\": \"Utilisateur non trouvé\"}");
                        return;
                    }
                    
                    context.setVariable("participant", user);
                    context.setVariable("isPublicView", false);
                    context.setVariable("isAdmin", isAdmin);
                    context.setVariable("isOwnProfile", isOwnProfile);
                }
            } else {
                // Affichage de son propre profil par défaut
                context.setVariable("participant", currentUser);
                context.setVariable("isPublicView", false);
                context.setVariable("isAdmin", authService.isAdmin(token));
                context.setVariable("isOwnProfile", true);
            }
            
            // Traitement de la page
            response.setContentType("text/html");
            templateEngine.process("profile", context, response.getWriter());
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID d'utilisateur invalide\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Traite les requêtes PUT pour mettre à jour le profil utilisateur
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            Participant currentUser = authService.getParticipantFromToken(token);
            if (currentUser == null) {
                throw new ServletException("Impossible de récupérer les informations de l'utilisateur");
            }
            
            String pathInfo = request.getPathInfo();
            int userId;
            
            // Détermination de l'utilisateur à modifier
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                userId = Integer.parseInt(pathInfo.substring(1));
                
                // Vérification des permissions
                boolean isAdmin = authService.isAdmin(token);
                boolean isOwnProfile = currentUser.getIdParticipant() == userId;
                
                if (!isAdmin && !isOwnProfile) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\": \"Vous n'êtes pas autorisé à modifier ce profil\"}");
                    return;
                }
            } else {
                // Par défaut, modifie son propre profil
                userId = currentUser.getIdParticipant();
            }
            
            // Récupération du participant à modifier
            Participant participant = participantService.getParticipantById(userId);
            if (participant == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Utilisateur non trouvé\"}");
                return;
            }
            
            // Mise à jour des champs du participant
            String nom = request.getParameter("nom");
            if (nom != null && !nom.trim().isEmpty()) {
                participant.setNom(nom);
            }
            
            String prenom = request.getParameter("prenom");
            if (prenom != null && !prenom.trim().isEmpty()) {
                participant.setPrenom(prenom);
            }
            
            // Mise à jour du mot de passe si fourni
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                // Vérification que le mot de passe actuel est correct
                if (currentPassword == null || !CryptUtil.checkPassword(currentPassword, participant.getMotDePasse())) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Le mot de passe actuel est incorrect\"}");
                    return;
                }
                
                // Vérification que les nouveaux mots de passe correspondent
                if (!newPassword.equals(confirmPassword)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Les nouveaux mots de passe ne correspondent pas\"}");
                    return;
                }
                
                // Chiffrement et mise à jour du mot de passe
                participant.setMotDePasse(CryptUtil.hashPassword(newPassword));
            }
            
            // Traitement de la photo de profil
            Part filePart = request.getPart("profilePicture");
            if (filePart != null && filePart.getSize() > 0) {
                String fileName = getFileName(filePart);
                
                if (fileName != null && !fileName.trim().isEmpty()) {
                    // Génération d'un nom de fichier unique
                    String extension = fileName.substring(fileName.lastIndexOf('.'));
                    String uniqueFileName = UUID.randomUUID().toString() + extension;
                    
                    // Chemin du fichier upload
                    String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
                    Path filePath = Paths.get(uploadPath, uniqueFileName);
                    
                    // Sauvegarde du fichier
                    Files.copy(filePart.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    
                    // Mise à jour de l'URL de profil
                    participant.setUrlProfile(UPLOAD_DIRECTORY + File.separator + uniqueFileName);
                }
            }
            
            // Sauvegarde des modifications
            participantService.updateParticipant(participant);
            
            // Réponse
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"message\": \"Profil mis à jour avec succès\"}");
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID d'utilisateur invalide\"}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Extrait le nom du fichier à partir de l'en-tête Content-Disposition
     */
    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] tokens = contentDisp.split(";");
        
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        
        return null;
    }
}
