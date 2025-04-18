package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.CauseService;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.service.impl.CauseServiceImpl;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
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
@WebServlet(name = "causeServlet", value = {"/causes", "/causes/*"})
public class CauseServlet extends HttpServlet {

    private AuthService authService;
    private CauseService causeService;
    private CourseService courseService;

    public void init() {
        authService = new AuthServiceImpl();
        causeService = new CauseServiceImpl();
        courseService = new CourseServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Récupération du moteur de template
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");

        // Préparation du contexte Thymeleaf
        Context context = new Context();

        // Récupération du token depuis les attributs de la requête (placé par le filtre)
        String token = (String) request.getAttribute("jwt_token");
        if (token != null) {
            Boolean isValid = authService.isTokenValid(token);
            if (!isValid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Token invalide\"}");
                return;
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Vérification si nous avons un ID de cause dans l'URL
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                // Extraction de l'ID de la cause à partir de l'URL (/causes/{id})
                int causeId = Integer.parseInt(pathInfo.substring(1));

                // Récupérer une cause spécifique
                causeService.getCauseById(causeId)
                        .ifPresentOrElse(
                                cause -> {
                                    try {
                                        response.setStatus(HttpServletResponse.SC_OK);
                                        response.getWriter().write(String.valueOf(cause));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                },
                                () -> {
                                    try {
                                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                        response.getWriter().write("{\"error\": \"Cause non trouvée avec l'ID " + causeId + "\"}");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"ID de cause invalide\"}");
            }
        } else {
            // Récupération des causes
            List<Cause> causes = causeService.getAllCauses();

            // Renvoyer la liste de toutes les causes
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(causes.toString());

            // Ajout des causes à la vue
            context.setVariable("causes", causes);

            // Traitement de la vue (liste des causes ou formulaire de création)
            templateEngine.process("causes", context, response.getWriter());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = (String) request.getAttribute("jwt_token");

        // Vérification que l'utilisateur est admin
        boolean isAdmin = authService.isAdmin(token);
        if (!isAdmin) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Seuls les administrateurs peuvent créer des causes\"}");
            return;
        }

        try {
            // Récupération du champ du formulaire
            String intitule = request.getParameter("intitule");
            String[] coursesIds = request.getParameterValues("coursesIds"); // tableau d'IDs de courses

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

            // Réponse
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"message\": \"Cause créée avec succès\"}");

            // Redirection après succès
            response.sendRedirect(request.getContextPath() + "/causes");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Récupération du token et vérification des permissions
        String token = (String) request.getAttribute("jwt_token");
        boolean isAdmin = authService.isAdmin(token);

        if (!isAdmin) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Permissions insuffisantes pour modifier une cause\"}");
            return;
        }

        try {
            // Extraction de l'ID de la cause à partir de l'URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.startsWith("/")) {
                throw new IllegalArgumentException("L'ID de la cause doit être spécifié dans l'URL");
            }

            int causeId = Integer.parseInt(pathInfo.substring(1));

            // Récupération de la cause existante
            Cause causeCourante = causeService.getCauseById(causeId)
                    .orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + causeId));

            // Mise à jour de l'intitulé
            String intitule = request.getParameter("intitule");
            if (intitule != null && !intitule.trim().isEmpty()) {
                causeCourante.setIntitule(intitule.trim());
            }

            // Mise à jour des courses associées
            String[] coursesIds = request.getParameterValues("coursesIds");
            if (coursesIds != null) {
                List<Course> selectedCourses = new ArrayList<>();

                for (String idStr : coursesIds) {
                    try {
                        int id = Integer.parseInt(idStr);
                        courseService.getCourseById(id).ifPresent(selectedCourses::add);
                    } catch (NumberFormatException e) {
                        System.err.println("ID de course invalide : " + idStr);
                    }
                }

                causeCourante.setCourses(selectedCourses);
            }

            // Enregistrement des modifications
            causeService.updateCause(causeCourante);

            // Réponse
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"message\": \"Cause mise à jour avec succès\"}");

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Valeur numérique invalide : " + e.getMessage() + "\"}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur s'est produite : " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Récupération du token et vérification des permissions
        String token = (String) request.getAttribute("jwt_token");
        boolean isAdmin = authService.isAdmin(token);

        if (!isAdmin) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Permissions insuffisantes pour supprimer une cause\"}");
            return;
        }

        try {
            // Extraction de l'ID de cause à partir de l'URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.startsWith("/")) {
                throw new IllegalArgumentException("L'ID de la cause doit être spécifié dans l'URL");
            }

            int causeId = Integer.parseInt(pathInfo.substring(1));

            // Récupération de la cause existante pour vérifier qu'elle existe
            causeService.getCauseById(causeId)
                    .orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + causeId));

            // Suppression de la cause
            boolean supprime = causeService.deleteCause(causeId);

            if (supprime) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\": \"Cause supprimée avec succès\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Échec de la suppression de la cause\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"ID de cause invalide\"}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur s'est produite : " + e.getMessage() + "\"}");
        }
    }
}
