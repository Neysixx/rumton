package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.CauseService;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.service.impl.CauseServiceImpl;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import fr.esgi.color_run.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet de gestion des causes
 */
@WebServlet(name = "causeServlet", value = {"/causes", "/api/causes"})
public class CauseServlet extends HttpServlet {

    private AuthService authService;

    public void init() {
        authService = new AuthServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, IOException {
        // Récupération du moteur de template
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");

        // Préparation du contexte Thymeleaf
        Context context = new Context();

        // Traitement de la vue (liste des causes ou formulaire de création)
        templateEngine.process("causes", context, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CauseService causeService = new CauseServiceImpl();
        CourseService courseService = new CourseServiceImpl();

        String token = (String) request.getAttribute("jwt_token");

        try {
            Boolean isAdmin = authService.isAdmin(token);
            // Récupération du champ du formulaire
            if (!isAdmin) {
                throw new IllegalArgumentException("Vous n'avez pas les droits nécessaires pour créer une cause.");
            }

            String intitule = request.getParameter("intitule");
            String[] coursesIds = request.getParameterValues("coursesIds"); // tableau d’IDs de courses

            // Validation
            if (intitule == null || intitule.trim().isEmpty()) {
                throw new IllegalArgumentException("L'intitulé de la cause est obligatoire");
            }

            List<Course> selectedCourses = new ArrayList<>();

            if (coursesIds != null) {
                for (String idStr : coursesIds) {
                    try {
                        int id = Integer.parseInt(idStr);
                        courseService.getCourseById(id).ifPresent(selectedCourses::add);
                    } catch (NumberFormatException e) {
                        System.err.println("ID de course invalide : " + idStr);
                    }
                }
            }

            // Création de l'objet Cause
            Cause cause = Cause.builder()
                    .intitule(intitule.trim())
                    .courses(selectedCourses)
                    .build();

            // Enregistrement
            causeService.createCause(cause);

            // Redirection après succès
            // response.sendRedirect(request.getContextPath() + "/causes");

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Erreur lors de la création de la cause : " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/templates/causes.html").forward(request, response);
        }
    }
}
