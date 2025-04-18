package fr.esgi.color_run.servlet;

import fr.esgi.color_run.repository.AdminRepository;
import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.repository.impl.AdminRepositoryImpl;
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

@WebServlet(name = "loginServlet", value = "/login")
public class LoginServlet extends HttpServlet {

    private LoginService loginService;
    private JwtUtil jwtUtil;

    @Override
    public void init() {
        ParticipantRepository participantRepository = new ParticipantRepositoryImpl();
        AdminRepository adminRepository = new AdminRepositoryImpl();
        jwtUtil = JwtUtil.getInstance();;
        loginService = new LoginServiceImpl(participantRepository, adminRepository, jwtUtil);
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
            Cookie jwtCookie = new Cookie("jwt_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 heures en secondes
            jwtCookie.setPath("/");
            // jwtCookie.setSecure(true); // À décommenter en production pour HTTPS
            response.addCookie(jwtCookie);

            // Réponse JSON avec le token et le rôle
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            String role = jwtUtil.getRoleFromToken(token);
            PrintWriter out = response.getWriter();
            out.print("{\"success\": true, \"token\": \"" + token + "\", \"role\": \"" + role + "\"}");
        } else {
            // En cas d'échec d'authentification
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print("{\"success\": false, \"error\": \"Identifiants invalides\"}");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
