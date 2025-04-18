package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.service.AdminService;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.impl.AdminServiceImpl;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.util.CryptUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.List;

/**
 * Servlet pour gérer les administrateurs
 */
@WebServlet(name = "adminServlet", value = {"/admins", "/admins/*"})
public class AdminServlet extends HttpServlet {

    private AuthService authService;
    private AdminService adminService;

    @Override
    public void init() {
        authService = new AuthServiceImpl();
        adminService = new AdminServiceImpl();
    }

    /**
     * Traite les requêtes GET pour afficher les administrateurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token) || !authService.isAdmin(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Droits d'administrateur requis\"}");
            return;
        }

        // On récupère le moteur de template dans le contexte des servlets
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        Context context = new Context();

        // Configuration de la réponse
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Analyse du chemin pour déterminer le type de requête
        String pathInfo = request.getPathInfo();

        try {
            // URL pattern: /admins/{id}
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                int adminId = Integer.parseInt(pathInfo.substring(1));
                
                // Récupération de l'administrateur
                Admin admin = adminService.getAdminById(adminId);
                if (admin == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Administrateur non trouvé\"}");
                    return;
                }
                
                // On ne renvoie pas le mot de passe
                admin.setMotDePasse(null);
                
                // Ajout à la vue
                context.setVariable("admin", admin);
                
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(admin.toString());
                templateEngine.process("admin", context, response.getWriter());
            }
            // URL pattern: /admins
            else {
                // Récupération de tous les administrateurs
                List<Admin> admins = adminService.getAllAdmins();
                
                // Masquer les mots de passe
                admins.forEach(a -> a.setMotDePasse(null));
                
                // Ajout à la vue
                context.setVariable("admins", admins);
                
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(admins.toString());
                templateEngine.process("admins", context, response.getWriter());
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token) || !authService.isAdmin(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Droits d'administrateur requis\"}");
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
                
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Tous les champs sont obligatoires\"}");
                return;
            }
            
            // Vérification si l'email est déjà utilisé
            if (adminService.existsByEmail(email)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Cette adresse email est déjà utilisée\"}");
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
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Traite les requêtes PUT pour mettre à jour un administrateur
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token) || !authService.isAdmin(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Droits d'administrateur requis\"}");
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
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Administrateur non trouvé\"}");
                return;
            }
            
            // Vérification que l'administrateur ne se supprime pas lui-même les droits
            Admin currentAdmin = authService.getAdminFromToken(token);
            if (currentAdmin != null && currentAdmin.getIdAdmin() == adminId) {
                // L'administrateur peut seulement modifier ses informations personnelles
                // mais pas son statut d'administrateur
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
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Cette adresse email est déjà utilisée\"}");
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
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID invalide\"}");
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
     * Traite les requêtes DELETE pour supprimer un administrateur
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification et des droits d'administrateur
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token) || !authService.isAdmin(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Droits d'administrateur requis\"}");
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
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Administrateur non trouvé\"}");
                return;
            }
            
            // Vérification que l'administrateur ne se supprime pas lui-même
            Admin currentAdmin = authService.getAdminFromToken(token);
            if (currentAdmin != null && currentAdmin.getIdAdmin() == adminId) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Un administrateur ne peut pas se supprimer lui-même\"}");
                return;
            }
            
            // Suppression de l'administrateur
            boolean supprime = adminService.deleteAdmin(adminId);
            
            if (supprime) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\": \"Administrateur supprimé avec succès\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Échec de la suppression de l'administrateur\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID invalide\"}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }
}
