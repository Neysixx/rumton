package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.service.CauseService;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.impl.CauseServiceImpl;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet de gestion des causes
 */
@WebServlet(name = "causeServlet", value = {"/causes", "/causes/*"})
public class CauseServlet extends BaseWebServlet {

    private CauseService causeService;
    private CourseService courseService;

    @Override
    public void init() {
        super.init();
        causeService = new CauseServiceImpl();
        courseService = new CourseServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        Context context = new Context();

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
                                        context.setVariable("cause", cause);
                                        context.setVariable("isAdmin", request.getAttribute("is_admin"));
                                        context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));
                                        renderTemplate(request, response, "cause_details", context);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (ServletException e) {
                                        e.printStackTrace();
                                    }
                                },
                                () -> {
                                    try {
                                        renderError(request, response, "Cause non trouvée avec l'ID " + causeId);
                                    } catch (IOException | ServletException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
            } catch (NumberFormatException e) {
                renderError(request, response, "ID de cause invalide");
            }
        } else {
            // Récupération des causes
            List<Cause> causes = causeService.getAllCauses();

            // Ajout des causes à la vue
            context.setVariable("causes", causes);
            context.setVariable("isAdmin", request.getAttribute("is_admin"));
            context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));

            // Rendu de la page
            renderTemplate(request, response, "causes", context);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // Vérification que l'utilisateur est admin
        if (!isAdmin(request, response)) {
            return;
        }

        try {
            // Récupération du champ du formulaire
            String intitule = request.getParameter("intitule");
            String[] coursesIds = request.getParameterValues("coursesIds"); // tableau d'IDs de courses

            // Validation
            if (intitule == null || intitule.trim().isEmpty()) {
                renderError(request, response, "L'intitulé de la cause est obligatoire");
                return;
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
            response.sendRedirect(request.getContextPath() + "/causes");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue : " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // Vérification que l'utilisateur est admin
        if (!isAdmin(request, response)) {
            return;
        }

        try {
            // Extraction de l'ID de la cause à partir de l'URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                renderError(request, response, "L'ID de la cause doit être spécifié dans l'URL");
                return;
            }

            int causeId = Integer.parseInt(pathInfo.substring(1));

            // Récupération de la cause existante
            Cause cause = causeService.getCauseById(causeId)
                    .orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + causeId));

            // Mise à jour de l'intitulé
            String intitule = request.getParameter("intitule");
            if (intitule != null && !intitule.trim().isEmpty()) {
                cause.setIntitule(intitule.trim());
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

                cause.setCourses(selectedCourses);
            }

            // Enregistrement des modifications
            causeService.updateCause(cause);

            // Redirection vers la page de détails de la cause
            response.sendRedirect(request.getContextPath() + "/causes/" + causeId);

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de cause invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue : " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // Vérification que l'utilisateur est admin
        if (!isAdmin(request, response)) {
            return;
        }

        try {
            // Extraction de l'ID de cause à partir de l'URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                renderError(request, response, "L'ID de la cause doit être spécifié dans l'URL");
                return;
            }

            int causeId = Integer.parseInt(pathInfo.substring(1));

            // Vérification que la cause existe
            if (!causeService.getCauseById(causeId).isPresent()) {
                renderError(request, response, "Cause non trouvée avec l'ID " + causeId);
                return;
            }

            // Suppression de la cause
            causeService.deleteCause(causeId);

            // Redirection vers la liste des causes
            response.sendRedirect(request.getContextPath() + "/causes");

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de cause invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue : " + e.getMessage());
        }
    }
}
