package fr.esgi.color_run.servlet;

import fr.esgi.color_run.repository.AdminRepository;
import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.repository.impl.AdminRepositoryImpl;
import fr.esgi.color_run.repository.impl.ParticipantRepositoryImpl;
import fr.esgi.color_run.service.LoginService;
import fr.esgi.color_run.service.impl.LoginServiceImpl;
import fr.esgi.color_run.util.CookieUtil;
import fr.esgi.color_run.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;

/**
 * Servlet de gestion de l'authentification utilisateur
 */
@WebServlet(name = "loginServlet", value = "/login")
public class LoginServlet extends BaseWebServlet  {

    private LoginService loginService;
    private JwtUtil jwtUtil;

    @Override
    public void init() {
        super.init();
        ParticipantRepository participantRepository = new ParticipantRepositoryImpl();
        AdminRepository adminRepository = new AdminRepositoryImpl();
        jwtUtil = JwtUtil.getInstance();;
        loginService = new LoginServiceImpl(participantRepository, adminRepository, jwtUtil);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Création du contexte Thymeleaf
        Context context = new Context();

        // Vérifier s'il y a un message d'erreur dans la session
        Object errorMessage = request.getSession().getAttribute("loginError");
        if (errorMessage != null) {
            context.setVariable("error", errorMessage);
            request.getSession().removeAttribute("loginError");
        }

        // Rendu de la page
        renderTemplate(request, response, "auth/login", context);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Context context = new Context();

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        // Type d'utilisateur (admin ou participant)
        String userType = request.getParameter("userType");
        
        String token = null;
        
        // Authentification basée sur le type d'utilisateur
        if ("admin".equals(userType)) {
            token = loginService.authenticateAdmin(email, password);
        } else {
            token = loginService.authenticateParticipant(email, password);
        }

        if (token != null) {
            // Stockage du token dans un cookie sécurisé
            CookieUtil.setCookie(response, CookieUtil.JWT_COOKIE_NAME, token, CookieUtil.JWT_COOKIE_MAX_AGE);

            // Redirection vers la page liste des courses
            response.sendRedirect(request.getContextPath() + "/courses");
        } else {
            // En cas d'échec d'authentification
            context.setVariable("error", "Les identifiants sont incorrects");
            renderTemplate(request, response, "auth/login", context);
        }
    }
}
