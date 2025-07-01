package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.DemandeOrganisateur;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Participation;
import fr.esgi.color_run.service.*;
import fr.esgi.color_run.service.impl.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private AdminService adminService;
    private CourseService courseService;
    private ParticipationService  participationService;
    private DemandeOrganisateurService demandeOrganisateurService;
    private static final String UPLOAD_DIRECTORY = "uploads";
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public void init() {
        super.init();
        participantService = new ParticipantServiceImpl();
        adminService = new AdminServiceImpl();
        courseService = new CourseServiceImpl();
        participationService = new ParticipationServiceImpl();
        demandeOrganisateurService = new DemandeOrganisateurServiceImpl();
        
        // Création du répertoire d'upload s'il n'existe pas
        try {
            String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
            File uploadDir = new File(uploadPath);
            
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (created) {
                    System.out.println("Répertoire d'upload créé: " + uploadPath);
                } else {
                    System.err.println("Impossible de créer le répertoire d'upload: " + uploadPath);
                }
            } else {
                System.out.println("Répertoire d'upload existant: " + uploadPath);
            }
            
            // Vérification des permissions
            if (!uploadDir.canWrite()) {
                System.err.println("ATTENTION: Pas de permission d'écriture sur le répertoire d'upload: " + uploadPath);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du répertoire d'upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traite les requêtes GET pour afficher le profil utilisateur
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Participant currentUser = getAuthenticatedParticipant(request);
            Admin currentAdmin = getAuthenticatedAdmin(request);
            if (currentUser == null && currentAdmin == null) {
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
                boolean isOrga = isOrganisateur(request, response);
                boolean isOwnProfile = currentAdmin != null ? currentAdmin.getIdAdmin() == userId : currentUser.getIdParticipant() == userId;
                
                if (!isAdmin && !isOwnProfile) {
                    // Affichage du profil public (moins d'informations)
                    Participant publicUser = participantService.getParticipantById(userId);
                    Admin publicAdmin = adminService.getAdminById(userId);
                    if (publicUser == null && publicAdmin == null) {
                        renderError(request, response, "Utilisateur non trouvé");
                        return;
                    }
                    if(publicAdmin != null) {
                        publicAdmin.setMotDePasse(null);
                        context.setVariable("user", publicAdmin);
                    }
                    else {
                        // On cache certaines informations sensibles
                        publicUser.setMotDePasse(null);
                        context.setVariable("user", publicUser);
                    }

                    context.setVariable("isPublicView", true);
                } else {
                    // Affichage du profil complet
                    Participant user;
                    Admin admin;

                    // Si c'est son propre profil, utiliser directement les objets de session pour éviter tout conflit d'ID
                    if (isOwnProfile) {
                        user = currentUser; // peut être null si admin
                        admin = currentAdmin; // peut être null si participant
                    } else {
                        // Sinon, charger depuis la base
                        user = participantService.getParticipantById(userId);
                        admin = adminService.getAdminById(userId);
                    }

                    if (user == null && admin == null) {
                        renderError(request, response, "Utilisateur non trouvé");
                        return;
                    }

                    List<Course> courses = new ArrayList<>();
                    if(isOrga && user != null) {
                        courses = courseService.getCoursesByOrgaId(user.getIdParticipant());
                    }
                    
                    // Récupération de l'état de la demande d'organisateur si applicable
                    DemandeOrganisateur demandeOrganisateur = null;
                    if (user != null && !user.isEstOrganisateur()) {
                        List<DemandeOrganisateur> demandes = demandeOrganisateurService.getDemandesByParticipant(user.getIdParticipant());
                        if (!demandes.isEmpty()) {
                            // Prendre la dernière demande (la plus récente)
                            demandeOrganisateur = demandes.get(0);
                        }
                    }
                    

                    context.setVariable("courses", courses);
                    context.setVariable("user", user == null ? admin : user);
                    context.setVariable("isPublicView", false);
                    context.setVariable("isAdmin", isAdmin);
                    context.setVariable("isOwnProfile", isOwnProfile);
                    context.setVariable("demandeOrganisateur", demandeOrganisateur);
                    if(isOwnProfile){
                        // Rendu de la page
                        renderTemplate(request, response, "profile/editProfile", context);
                        return;
                    }
                }
            } else {
                // Affichage de son propre profil par défaut
                boolean isAdmin = isAdmin(request, response);
                List<Course> courses = new ArrayList<>();
                Object isOrgaAttribute = request.getAttribute("is_organisateur");
                boolean isOrga = isOrgaAttribute != null && Boolean.parseBoolean(isOrgaAttribute.toString());
                
                if(isOrga && currentUser != null) {
                    courses = courseService.getCoursesByOrgaId(currentUser.getIdParticipant());
                }
                else if(!isOrga && !isAdmin && currentUser != null){
                    List<Participation> participations = participationService.getParticipationsByParticipant(currentUser.getIdParticipant());
                    courses = participations.stream()
                            .map(Participation::getCourse)
                            .distinct()
                            .collect(Collectors.toList());
                }
                
                // Récupération de l'état de la demande d'organisateur si applicable
                DemandeOrganisateur demandeOrganisateur = null;
                if (currentUser != null && !currentUser.isEstOrganisateur()) {
                    List<DemandeOrganisateur> demandes = demandeOrganisateurService.getDemandesByParticipant(currentUser.getIdParticipant());
                    if (!demandes.isEmpty()) {
                        // Prendre la dernière demande (la plus récente)
                        demandeOrganisateur = demandes.get(0);
                    }
                }
                
                context.setVariable("user", currentUser == null ? currentAdmin : currentUser);
                context.setVariable("isPublicView", false);
                context.setVariable("isAdmin", request.getAttribute("is_admin"));
                context.setVariable("isOwnProfile", true);
                context.setVariable("isDemandeExist", currentUser != null && demandeOrganisateurService.hasDemandeEnCours(currentUser.getIdParticipant()));
                context.setVariable("demandeOrganisateur", demandeOrganisateur);
                if(currentUser != null) {
                    courses.forEach(course -> {
                        int participationId = participationService.getParticipationIdByCourseAndParticipant(course.getIdCourse(), currentUser.getIdParticipant());
                        course.setParticipationIdUser(participationId);
                    });
                }
                context.setVariable("courses", courses);
            }
            
            // Rendu de la page
            renderTemplate(request, response, "profile/profile", context);
            
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

        try {
            Participant currentUser = getAuthenticatedParticipant(request);
            Admin currentAdmin = getAuthenticatedAdmin(request);
            if (currentUser == null && currentAdmin == null) {
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
                boolean isOwnProfile = currentAdmin != null ? currentAdmin.getIdAdmin() == userId : currentUser.getIdParticipant() == userId;
                
                if (!isAdmin && !isOwnProfile) {
                    renderError(request, response, "Vous n'êtes pas autorisé à modifier ce profil");
                    return;
                }
            } else {
                // Par défaut, modifie son propre profil
                userId = currentAdmin != null ? currentAdmin.getIdAdmin() : currentUser.getIdParticipant();
            }
            
            // Récupération du participant à modifier
            Participant participant = participantService.getParticipantById(userId);
            Admin admin = adminService.getAdminById(userId);
            if (participant == null && admin == null) {
                renderError(request, response, "Utilisateur non trouvé");
                return;
            }
            
            // Mise à jour des champs du participant
            String nom = request.getParameter("nom");
            if (nom != null && !nom.trim().isEmpty()) {
                if(admin == null) {
                    participant.setNom(nom);
                }
                else {
                    admin.setNom(nom);
                }
            }
            
            String prenom = request.getParameter("prenom");
            if (prenom != null && !prenom.trim().isEmpty()) {
                if(admin == null) {
                    participant.setPrenom(prenom);
                }
                else {
                    admin.setPrenom(prenom);
                }
            }
            
            // Mise à jour du mot de passe si fourni
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                // Vérification que le mot de passe actuel est correct
                if (currentPassword == null || !CryptUtil.checkPassword(currentPassword, admin != null ? admin.getMotDePasse() : participant.getMotDePasse())) {
                    renderError(request, response, "Le mot de passe actuel est incorrect");
                    return;
                }
                
                // Vérification que les nouveaux mots de passe correspondent
                if (!newPassword.equals(confirmPassword)) {
                    renderError(request, response, "Les nouveaux mots de passe ne correspondent pas");
                    return;
                }
                
                // Chiffrement et mise à jour du mot de passe
                if(admin == null) {
                    participant.setMotDePasse(CryptUtil.hashPassword(newPassword));
                }
                else {
                    admin.setMotDePasse(CryptUtil.hashPassword(newPassword));
                }
            }
            
            // Traitement de la photo de profil
            Part filePart = request.getPart("profilePicture");
            if (filePart != null && filePart.getSize() > 0) {
                try {
                    validateImageFile(filePart);
                    
                    String fileName = getFileName(filePart);
                    System.out.println("Traitement de la photo de profil: " + fileName + " (" + filePart.getSize() + " bytes)");
                    
                    if (fileName != null && !fileName.trim().isEmpty()) {
                        // Supprimer l'ancienne photo de profil
                        String oldUrlProfile = admin != null ? admin.getUrlProfile() : participant.getUrlProfile();
                        deleteOldProfilePicture(oldUrlProfile);
                        
                        // Génération d'un nom de fichier unique
                        String extension = fileName.substring(fileName.lastIndexOf('.'));
                        String uniqueFileName = UUID.randomUUID().toString() + extension;
                        
                        // Chemin du fichier upload
                        String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
                        File uploadDir = new File(uploadPath);
                        
                        // Vérification du répertoire
                        if (!uploadDir.exists()) {
                            boolean created = uploadDir.mkdirs();
                            if (!created) {
                                throw new IOException("Impossible de créer le répertoire d'upload: " + uploadPath);
                            }
                        }
                        
                        if (!uploadDir.canWrite()) {
                            throw new IOException("Pas de permission d'écriture sur le répertoire d'upload: " + uploadPath);
                        }
                        
                        Path filePath = Paths.get(uploadPath, uniqueFileName);
                        
                        // Sauvegarde du fichier
                        Files.copy(filePart.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Photo de profil sauvegardée: " + filePath.toString());
                        
                        // URL relative pour la base de données
                        String relativeUrl = "/color_run_war_exploded/" + UPLOAD_DIRECTORY + "/" + uniqueFileName;
                        
                        // Mise à jour de l'URL de profil
                        if(admin == null) {
                            participant.setUrlProfile(relativeUrl);
                        }
                        else {
                            admin.setUrlProfile(relativeUrl);
                        }
                        System.out.println("URL de profil mise à jour: " + relativeUrl);
                    }
                } catch (IllegalArgumentException e) {
                    renderError(request, response, e.getMessage());
                    return;
                } catch (IOException e) {
                    System.err.println("Erreur lors de la sauvegarde de l'image: " + e.getMessage());
                    renderError(request, response, "Erreur lors de la sauvegarde de l'image: " + e.getMessage());
                    return;
                }
            }
            
            // Sauvegarde des modifications
            if(admin == null) {
                participantService.updateParticipant(participant);
            }
            else {
                adminService.updateAdmin(admin);
            }
            
            // Redirection vers la page de profil
            response.sendRedirect(request.getContextPath() + "/profile");
            
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

    /**
     * Valide le fichier image uploadé
     */
    private void validateImageFile(Part filePart) throws IllegalArgumentException {
        if (filePart.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Le fichier est trop volumineux. Taille maximale : 5MB");
        }
        
        String fileName = getFileName(filePart);
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }
        
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        boolean isValidExtension = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                isValidExtension = true;
                break;
            }
        }
        
        if (!isValidExtension) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Extensions autorisées : jpg, jpeg, png, gif");
        }
    }
    
    /**
     * Supprime l'ancienne photo de profil si elle existe
     */
    private void deleteOldProfilePicture(String oldUrlProfile) {
        if (oldUrlProfile != null && !oldUrlProfile.contains("defaultProfile.png")) {
            try {
                // Extraire juste le nom du fichier depuis l'URL
                // oldUrlProfile format: "/color_run_war_exploded/uploads/filename.jpg"
                String fileName = null;
                if (oldUrlProfile.contains(UPLOAD_DIRECTORY + "/")) {
                    fileName = oldUrlProfile.substring(oldUrlProfile.lastIndexOf("/") + 1);
                }
                
                if (fileName != null && !fileName.trim().isEmpty()) {
                    String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
                    File oldFile = new File(uploadPath, fileName);
                    
                    if (oldFile.exists() && oldFile.isFile()) {
                        boolean deleted = oldFile.delete();
                        if (deleted) {
                            System.out.println("Ancienne photo supprimée : " + fileName);
                        } else {
                            System.err.println("Impossible de supprimer l'ancienne photo : " + fileName);
                        }
                    }
                }
            } catch (Exception e) {
                // Log l'erreur mais ne pas interrompre le processus
                System.err.println("Erreur lors de la suppression de l'ancienne photo : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
