package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.service.AdminService;
import fr.esgi.color_run.service.impl.AdminServiceImpl;
import fr.esgi.color_run.util.CryptUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.List;

/**
 * Servlet pour gérer les administrateurs
 */
@WebServlet(name = "adminServlet", value = {"/admins", "/admins/*"})
public class AdminServlet extends BaseWebServlet {
    private AdminService adminService;

    @Override
    public void init() {
        super.init();
        adminService = new AdminServiceImpl();
    }

    /**
     * Traite les requêtes GET pour afficher les administrateurs
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        if (!isAuthenticated(request, response)) {
            return;
        }

        Context context = new Context();

        // Analyse du chemin pour déterminer le type de requête
        String pathInfo = request.getPathInfo();
        try {
            // URL pattern: /admins/{id}
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                int adminId = Integer.parseInt(pathInfo.substring(1));
                
                // Récupération de l'administrateur
                Admin admin = adminService.getAdminById(adminId);
                if (admin == null) {
                    renderError(request, response, "Administrateur non trouvé");
                    return;
                }
                
                // On ne renvoie pas le mot de passe
                admin.setMotDePasse(null);
                
                // Ajout à la vue
                context.setVariable("admin", admin);
                
                renderTemplate(request, response, "admin", context);
            }
            // URL pattern: /admins
            else {
                // Récupération de tous les administrateurs
                List<Admin> admins = adminService.getAllAdmins();
                
                // Masquer les mots de passe
                admins.forEach(a -> a.setMotDePasse(null));
                
                // Ajout à la vue
                context.setVariable("admins", admins);
                
                renderTemplate(request, response, "admins", context);
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID invalide\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Traite les requêtes POST pour créer un nouvel administrateur
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        if (!isAuthenticated(request, response)) {
            renderError(request, response, "Droits d'administrateur requis");
            return;
        }

        try {
            // Récupération des paramètres du formulaire
            String nom = request.getParameter("nom");
            String prenom = request.getParameter("prenom");
            String email = request.getParameter("email");
            String motDePasse = request.getParameter("motDePasse");
            
            // Validation des données
            if (nom == null || prenom == null || email == null || motDePasse == null ||
                nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || motDePasse.isEmpty()) {
                renderError(request, response, "Tous les champs sont obligatoires");
                return;
            }
            
            // Vérification si l'email est déjà utilisé
            if (adminService.existsByEmail(email)) {
                renderError(request, response, "Cette adresse email est déjà utilisée");
                return;
            }
            
            // Création de l'administrateur
            Admin admin = Admin.builder()
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .motDePasse(CryptUtil.hashPassword(motDePasse))
                    .build();
            
            // Enregistrement de l'administrateur
            adminService.createAdmin(admin);
            
            // Réponse
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"message\": \"Administrateur créé avec succès\", \"id\": " + admin.getIdAdmin() + "}");
            
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Traite les requêtes PUT pour mettre à jour un administrateur
     */
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        if (!isAuthenticated(request, response)) {
            renderError(request, response, "Droits d'administrateur requis");
            return;
        }
        
        try {
            // Vérification du chemin
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new IllegalArgumentException("ID d'administrateur invalide ou manquant");
            }
            
            int adminId = Integer.parseInt(pathInfo.substring(1));
            
            // Récupération de l'administrateur
            Admin admin = adminService.getAdminById(adminId);
            if (admin == null) {
                renderError(request, response, "Administrateur non trouvé");
                return;
            }
            
            // Vérification que l'administrateur ne se supprime pas lui-même les droits
            Admin currentAdmin = getAuthenticatedAdmin(request);
            if (currentAdmin != null && currentAdmin.getIdAdmin() == adminId) {
                // L'administrateur peut seulement modifier ses informations personnelles
                // mais pas son statut d'administrateur
                renderError(request, response, "Vous ne pouvez pas modifier votre statut d'administrateur");
                return;
            }
            
            // Mise à jour des champs
            String nom = request.getParameter("nom");
            if (nom != null && !nom.trim().isEmpty()) {
                admin.setNom(nom);
            }
            
            String prenom = request.getParameter("prenom");
            if (prenom != null && !prenom.trim().isEmpty()) {
                admin.setPrenom(prenom);
            }
            
            String email = request.getParameter("email");
            if (email != null && !email.trim().isEmpty() && !email.equals(admin.getEmail())) {
                // Vérifier que l'email n'est pas déjà utilisé
                if (adminService.existsByEmail(email)) {
                    renderError(request, response, "Cette adresse email est déjà utilisée");
                    return;
                }
                admin.setEmail(email);
            }
            
            // Mise à jour du mot de passe si fourni
            String motDePasse = request.getParameter("motDePasse");
            if (motDePasse != null && !motDePasse.trim().isEmpty()) {
                admin.setMotDePasse(CryptUtil.hashPassword(motDePasse));
            }
            
            // Enregistrement des modifications
            adminService.updateAdmin(admin);
            
            // Réponse
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"message\": \"Administrateur mis à jour avec succès\"}");
            
        } catch (NumberFormatException e) {
            renderError(request, response, "Format d'ID invalide");
        } catch (IllegalArgumentException e) {
            renderError(request, response, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Traite les requêtes DELETE pour supprimer un administrateur
     */
    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        if (!isAuthenticated(request, response)) {
            renderError(request, response, "Droits d'administrateur requis");
            return;
        }
        
        try {
            // Récupération de l'ID de l'administrateur
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new IllegalArgumentException("ID d'administrateur invalide ou manquant");
            }
            
            int adminId = Integer.parseInt(pathInfo.substring(1));
            
            // Vérification que l'administrateur existe
            Admin admin = adminService.getAdminById(adminId);
            if (admin == null) {
                renderError(request, response, "Administrateur non trouvé");
                return;
            }
            
            // Vérification que l'administrateur ne se supprime pas lui-même
            Admin currentAdmin = getAuthenticatedAdmin(request);
            if (currentAdmin != null && currentAdmin.getIdAdmin() == adminId) {
                renderError(request, response, "Un administrateur ne peut pas se supprimer lui-même");
                return;
            }
            
            // Suppression de l'administrateur
            boolean supprime = adminService.deleteAdmin(adminId);
            
            if (supprime) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\": \"Administrateur supprimé avec succès\"}");
            } else {
                renderError(request, response, "Échec de la suppression de l'administrateur");
            }
            
        } catch (NumberFormatException e) {
            renderError(request, response, "Format d'ID invalide");
        } catch (IllegalArgumentException e) {
            renderError(request, response, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }
}
