package fr.esgi.color_run.servlet;

import java.io.*;
import java.sql.Timestamp;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.CauseService;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
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
    private AuthService authService;
    private CourseService courseService;
    private CauseService causeService;

    public void init() {
        message = "Liste des courses";
        authService = new AuthServiceImpl();
        courseService = new CourseServiceImpl();
        causeService = new CauseServiceImpl();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // On récupère le moteur de template dans le contexte des servlets
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");

        // On crée un context Thymeleaf qui va accueille des objets Java
        // qui seront envoyés à la vue Thymeleaf
        Context context = new Context();
        
        // On récupère le token depuis les attributs de la requête (placé par le filtre)
        String token = (String) request.getAttribute("jwt_token");
        if (token != null) {
            // Ajout d'informations supplémentaires au contexte si l'utilisateur est authentifié
            boolean isOrganisateur = (boolean) request.getAttribute("is_organisateur");
            boolean isAdmin = (boolean) request.getAttribute("is_admin");
            context.setVariable("isOrganisateur", isOrganisateur);
            context.setVariable("isAdmin", isAdmin);
            
            // Si l'utilisateur est un participant, on récupère ses données
            Participant participant = authService.getParticipantFromToken(token);
            if (participant != null) {
                context.setVariable("participant", participant);
            }
        }
        
        // Ajout des courses à la vue
        context.setVariable("courses", courseService.getAllCourses());

        // On invoque la méthode process qui formule la réponse qui sera renvoyée au navigateur
        templateEngine.process("courses", context, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Récupération du token depuis les attributs de la requête (placé par le filtre)
        String token = (String) request.getAttribute("jwt_token");
        
        // Vérification que l'utilisateur est un organisateur
        boolean isOrganisateur = (boolean) request.getAttribute("is_organisateur");
        if (!isOrganisateur) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Seuls les organisateurs peuvent créer des courses\"}");
            return;
        }

        try {
            // Récupération de l'organisateur à partir du token
            Participant organisateur = authService.getParticipantFromToken(token);
            if (organisateur == null) {
                throw new ServletException("Impossible de récupérer les informations de l'organisateur");
            }

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

            // Création de l'objet Course avec l'organisateur authentifié
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
                    .organisateur(organisateur)  // Affectation de l'organisateur
                    .build();

            // Enregistrement
            courseService.createCourse(course);

            // Redirection
            response.sendRedirect(request.getContextPath() + "/courses");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
