package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.service.AdminService;
import fr.esgi.color_run.service.impl.AdminServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

import static fr.esgi.color_run.util.CryptUtil.hashPassword;

/**
 * Servlet qui gère l'inscription des administrateurs
 */
@WebServlet(name = "registerAdminServlet", value = "/register-admin")
public class RegisterAdminServlet extends HttpServlet {

    private AdminService adminService;

    @Override
    public void init() {
        adminService = new AdminServiceImpl();
    }

    /**
     * Affiche le formulaire d'inscription admin
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        Context context = new Context();
        templateEngine.process("register-admin", context, response.getWriter());
    }

    /**
     * Traite les données du formulaire d'inscription admin
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String nom = request.getParameter("nom");
        String prenom = request.getParameter("prenom");
        String email = request.getParameter("email");
        String motDePasse = request.getParameter("motDePasse");
        String confirmMotDePasse = request.getParameter("confirmMotDePasse");

        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        Context context = new Context();

        if (nom == null || prenom == null || email == null || motDePasse == null ||
                nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || motDePasse.isEmpty()) {

            context.setVariable("error", "Tous les champs sont obligatoires");
            templateEngine.process("register-admin", context, response.getWriter());
            return;
        }

        if (!motDePasse.equals(confirmMotDePasse)) {
            context.setVariable("error", "Les mots de passe ne correspondent pas");
            templateEngine.process("register-admin", context, response.getWriter());
            return;
        }

        Admin admin = Admin.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .motDePasse(hashPassword(motDePasse))
                .urlProfile(null) // ou une valeur par défaut si besoin
                .build();

        try {
            adminService.createAdmin(admin);
            response.sendRedirect(request.getContextPath() + "/login-admin?registered=true");
        } catch (Exception e) {
            context.setVariable("error", "Une erreur est survenue lors de l'inscription : " + e.getMessage());
            templateEngine.process("register-admin", context, response.getWriter());
        }
    }
}
