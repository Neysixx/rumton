package fr.esgi.color_run.servlet;

import fr.esgi.color_run.repository.AdminRepository;
import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.repository.impl.AdminRepositoryImpl;
import fr.esgi.color_run.repository.impl.ParticipantRepositoryImpl;
import fr.esgi.color_run.service.impl.LoginServiceImpl;
import fr.esgi.color_run.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet de gestion de l'authentification utilisateur
 */
@WebServlet(name = "logoutServlet", value = "/logout")
public class LogoutServlet extends BaseWebServlet {

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Cookie cookie = new Cookie("jwt_token", "");
        cookie.setMaxAge(0); // Expire immédiatement
        cookie.setPath("/"); // Doit matcher le path d'origine
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // même logique que pour le login
        response.addCookie(cookie);

        // Redirige vers la page de login ou d'accueil
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
