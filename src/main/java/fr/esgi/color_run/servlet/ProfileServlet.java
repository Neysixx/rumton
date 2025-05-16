package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;
import fr.esgi.color_run.util.CryptUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
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
public class ProfileServlet extends BaseWebServlet {

    private ParticipantService participantService;
    private static final String UPLOAD_DIRECTORY = "uploads";

    @Override
    public void init() {
        super.init();
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
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            Participant currentUser = getAuthenticatedParticipant(request);
            if (currentUser == null) {
                renderError(request, response, "Impossible de récupérer les informations de l'utilisateur");
                return;
            }
            
            String pathInfo = request.getPathInfo();
            Context context = new Context();
            
            // Si l'URL contient un ID utilisateur, afficher ce profil spécifique
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                int userId = Integer.parseInt(pathInfo.substring(1));
                
                // Vérifier si on est admin ou si on consulte son propre profil
                boolean isAdmin = isAdmin(request, response);
                boolean isOwnProfile = currentUser.getIdParticipant() == userId;
                
                if (!isAdmin && !isOwnProfile) {
                    // Affichage du profil public (moins d'informations)
                    Participant publicUser = participantService.getParticipantById(userId);
                    if (publicUser == null) {
                        renderError(request, response, "Utilisateur non trouvé");
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
                        renderError(request, response, "Utilisateur non trouvé");
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
                context.setVariable("isAdmin", request.getAttribute("is_admin"));
                context.setVariable("isOwnProfile", true);
            }
            
            // Rendu de la page
            renderTemplate(request, response, "profile", context);
            
        } catch (NumberFormatException e) {
            renderError(request, response, "Format d'ID d'utilisateur invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Traite les requêtes POST pour mettre à jour le profil utilisateur
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            Participant currentUser = getAuthenticatedParticipant(request);
            if (currentUser == null) {
                renderError(request, response, "Impossible de récupérer les informations de l'utilisateur");
                return;
            }
            
            String pathInfo = request.getPathInfo();
            int userId;
            
            // Détermination de l'utilisateur à modifier
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                userId = Integer.parseInt(pathInfo.substring(1));
                
                // Vérification des permissions
                boolean isAdmin = isAdmin(request, response);
                boolean isOwnProfile = currentUser.getIdParticipant() == userId;
                
                if (!isAdmin && !isOwnProfile) {
                    renderError(request, response, "Vous n'êtes pas autorisé à modifier ce profil");
                    return;
                }
            } else {
                // Par défaut, modifie son propre profil
                userId = currentUser.getIdParticipant();
            }
            
            // Récupération du participant à modifier
            Participant participant = participantService.getParticipantById(userId);
            if (participant == null) {
                renderError(request, response, "Utilisateur non trouvé");
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
                    renderError(request, response, "Le mot de passe actuel est incorrect");
                    return;
                }
                
                // Vérification que les nouveaux mots de passe correspondent
                if (!newPassword.equals(confirmPassword)) {
                    renderError(request, response, "Les nouveaux mots de passe ne correspondent pas");
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
            
            // Redirection vers la page de profil
            response.sendRedirect(request.getContextPath() + "/profile/" + userId);
            
        } catch (NumberFormatException e) {
            renderError(request, response, "Format d'ID d'utilisateur invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "";
    }
}
