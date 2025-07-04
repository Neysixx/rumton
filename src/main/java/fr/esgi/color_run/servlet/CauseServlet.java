package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.service.CauseService;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.impl.CauseServiceImpl;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servlet de gestion des causes
 */
@WebServlet(name = "causeServlet", value = {"/causes", "/causes/*", "/causes-create", "/causes-edit/*"})
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
        Context context = new Context();
        
        // Vérifier l'authentification pour déterminer les droits
        final Boolean isAuthenticated = isAuthenticated(request, response);
        final Boolean isAdmin;
        final Boolean isOrganisateur;
        
        if (isAuthenticated) {
            isAdmin = Boolean.parseBoolean(request.getAttribute("is_admin").toString());
            isOrganisateur = Boolean.parseBoolean(request.getAttribute("is_organisateur").toString());
        } else {
            isAdmin = false;
            isOrganisateur = false;
        }

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
                                        context.setVariable("isAdmin", isAdmin);
                                        context.setVariable("isOrganisateur", isOrganisateur);
                                        if(request.getServletPath().equals("/causes-edit")) {
                                            // L'édition nécessite d'être admin
                                            if (!isAuthenticated || !isAdmin) {
                                                try {
                                                    response.sendRedirect(request.getContextPath() + "/causes");
                                                    return;
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            renderTemplate(request, response, "causes/edit", context);
                                        }else{
                                            context.setVariable("courses", courseService.getCoursesByCauseId(causeId));
                                            renderTemplate(request, response, "causes/cause_details", context);
                                        }
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
        } else if (request.getServletPath().equals("/causes-create")) {
            // La création nécessite d'être authentifié et admin
            if (!isAuthenticated || !isAdmin) {
                response.sendRedirect(request.getContextPath() + "/causes");
                return;
            }
            renderTemplate(request, response, "causes/create", context);
        } else {
            // Récupération des causes (accessible à tous)
            List<Cause> causes = causeService.getAllCauses();

            // Ajout des causes à la vue
            context.setVariable("causes", causes);
            context.setVariable("isAdmin", isAdmin);
            context.setVariable("isOrganisateur", isOrganisateur);

            // Rendu de la page
            renderTemplate(request, response, "causes/list", context);
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

            // Validation
            if (intitule == null || intitule.trim().isEmpty()) {
                renderError(request, response, "L'intitulé de la cause est obligatoire");
                return;
            }
            // Création de l'objet Cause
            Cause cause = Cause.builder()
                    .intitule(intitule.trim())
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
            BufferedReader reader = request.getReader();
            String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            Map<String, String> params = parseUrlEncodedBody(body);
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
            String intitule = params.get("intitule");
            if (intitule != null && !intitule.trim().isEmpty()) {
                cause.setIntitule(intitule.trim());
            }

            // Enregistrement des modifications
            causeService.updateCause(cause);

            response.setStatus(HttpServletResponse.SC_OK);

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
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de cause invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue : " + e.getMessage());
        }
    }
}
