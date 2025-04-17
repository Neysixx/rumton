package fr.esgi.color_run.servlet;

import java.io.*;
import java.sql.Timestamp;

//import fr.esgi.color_run.service.CourseService;
//import fr.esgi.color_run.service.impl.CourseServiceImpl;
import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.CauseService;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.impl.CauseServiceImpl;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Cette classe aura le pouvoir de traiter des requêtes HTTP
 */
@WebServlet(name = "coursesServlet", value = {"/index", "/api/courses"})
public class CourseServlet extends HttpServlet {
    private String message;

    public void init() {
        message = "Liste des courses";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // On récupère le moteur de template dans le contexte des servlets
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");

        // On crée un context Thymeleaf qui va accueille des objets Java
        // qui seront envoyés à la vue Thymeleaf
        Context context = new Context();

        // On invoque la méthode process qui formule la réponse qui sera renvoyée au navigateur
        templateEngine.process("courses", context, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CourseService courseService = new CourseServiceImpl();
        CauseService causeService = new CauseServiceImpl();

        // TODO !! Dans cette méthode l'organisateur sera toujours envoyé à null --> à préciser

        try {
            // Récupération des paramètres
            String nom = request.getParameter("nom");
            String description = request.getParameter("description");
            String dateDepartStr = request.getParameter("dateDepart");
            String ville = request.getParameter("ville");
            String codePostalStr = request.getParameter("codePostal");
            String adresse = request.getParameter("adresse");
            String distanceStr = request.getParameter("distance");
            String maxParticipantsStr = request.getParameter("maxParticipants");
            String prixParticipationStr = request.getParameter("prixParticipation");
            String obstacles = request.getParameter("obstacles");
            String idCauseStr = request.getParameter("idCause");

            // Conversions
            Timestamp dateDepart = Timestamp.valueOf(dateDepartStr + " 00:00:00");
            int codePostal = Integer.parseInt(codePostalStr);
            float distance = Float.parseFloat(distanceStr);
            int maxParticipants = Integer.parseInt(maxParticipantsStr);
            float prixParticipation = Float.parseFloat(prixParticipationStr);
            int idCause = Integer.parseInt(idCauseStr);

            // Recherche de la cause
            Cause cause;
            try {
                cause = causeService.getCauseById(idCause)
                        .orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + idCause));
            } catch (Exception e) {
                throw new ServletException("Impossible de récupérer la cause associée : " + e.getMessage());
            }

            // (Optionnel) Recherche de l'organisateur :
            Participant organisateur = null; // à récupérer via un service si nécessaire

            // Création de l'objet Course
            Course course = Course.builder()
                    .nom(nom)
                    .description(description)
                    .dateDepart(dateDepart)
                    .ville(ville)
                    .codePostal(codePostal)
                    .adresse(adresse)
                    .distance(distance)
                    .maxParticipants(maxParticipants)
                    .prixParticipation(prixParticipation)
                    .obstacles(obstacles)
                    .cause(cause)
                    .organisateur(organisateur)
                    .build();

            // Enregistrement
            courseService.createCourse(course);

            // Redirection
            response.sendRedirect(request.getContextPath() + "/courses");

        } catch (Exception e) {
            e.printStackTrace();

            request.setAttribute("error", "Erreur lors de la création de la course : " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/templates/courses.html").forward(request, response);
        }
    }
}
