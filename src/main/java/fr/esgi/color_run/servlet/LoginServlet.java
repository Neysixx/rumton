package fr.esgi.color_run.servlet;

import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.repository.impl.ParticipantRepositoryImpl;
import fr.esgi.color_run.service.LoginService;
import fr.esgi.color_run.service.impl.LoginServiceImpl;
import fr.esgi.color_run.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "loginServlet", value = "/api/login")
public class LoginServlet extends HttpServlet {

    private LoginService loginService;

    @Override
    public void init() {
        ParticipantRepository participantRepository = new ParticipantRepositoryImpl();
        JwtUtil jwtUtil = new JwtUtil();
        loginService = new LoginServiceImpl(participantRepository, jwtUtil);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Redirection vers la page de login
        request.getRequestDispatcher("/WEB-INF/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        System.out.println("Email: " + email);

        String token = null;
        token = loginService.authenticateParticipant(email, password);

        if (token != null) {
            // Stockage du token dans un cookie sécurisé
            Cookie jwtCookie = new Cookie("jwt_token", token);
            jwtCookie.setHttpOnly(true); // Empêche l'accès au cookie via JavaScript
            jwtCookie.setMaxAge(7200); // 2 heures en secondes
            jwtCookie.setPath("/");
            // jwtCookie.setSecure(true); // À décommenter en production pour HTTPS
            response.addCookie(jwtCookie);

            // Redirection vers la page d'accueil ou une page sécurisée
//            response.sendRedirect(request.getContextPath() + "/dashboard");
        } else {
            // En cas d'échec d'authentification
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print("{\"error\": \"Identifiants invalides\"}");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
