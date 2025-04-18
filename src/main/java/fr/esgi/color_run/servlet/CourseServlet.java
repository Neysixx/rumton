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
@WebServlet(name = "courseServlet", value = {"/courses", "/courses/*"})
public class CourseServlet extends HttpServlet {
    private AuthService authService;
    private CourseService courseService;
    private CauseService causeService;

    public void init() {
        authService = new AuthServiceImpl();
        courseService = new CourseServiceImpl();
        causeService = new CauseServiceImpl();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Cette méthode est appelée lorsqu'une requête GET est envoyée vers le servlet
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // On récupère le moteur de template dans le contexte des servlets
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");

        // On crée un context Thymeleaf qui va accueille des objets Java
        // qui seront envoyés à la vue Thymeleaf
        Context context = new Context();

        // On récupère le token depuis les attributs de la requête (placé par le filtre)
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

        // Vérification si nous avons un ID de course dans l'URL
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                // Extraction de l'ID de la course à partir de l'URL (/courses/{id})
                int courseId = Integer.parseInt(pathInfo.substring(1));

                // Récupérer une course spécifique
                courseService.getCourseById(courseId)
                        .ifPresentOrElse(
                                course -> {
                                    try {
                                        response.setStatus(HttpServletResponse.SC_OK);
                                        response.getWriter().write(String.valueOf(course));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                },
                                () -> {
                                    try {
                                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                        response.getWriter().write("{\"error\": \"Course non trouvée avec l'ID " + courseId + "\"}");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"ID de course invalide\"}");
            }
        } else {
            // Renvoyer la liste de toutes les courses
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(courseService.getAllCourses().toString());

            // Ajout des courses à la vue
            context.setVariable("courses", courseService.getAllCourses());

            // On invoque la méthode process qui formule la réponse qui sera renvoyée au navigateur
            templateEngine.process("courses", context, response.getWriter());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Récupération du token depuis les attributs de la requête (placé par le filtre)
        String token = (String) request.getAttribute("jwt_token");

        // Vérification que l'utilisateur est un organisateur
        boolean isOrganisateur = authService.isOrganisateur(token);
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

            System.out.println("Course créée : " + course);

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

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Récupération du token et vérification des permissions
        String token = (String) request.getAttribute("jwt_token");
        boolean isOrganisateur = authService.isOrganisateur(token);
        boolean isAdmin = authService.isAdmin(token);

        if (!isOrganisateur && !isAdmin) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Permissions insuffisantes pour modifier une course\"}");
            return;
        }

        try {
            // Extraction de l'ID de la course à partir de l'URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.startsWith("/")) {
                throw new IllegalArgumentException("L'ID de la course doit être spécifié dans l'URL");
            }

            int courseId = Integer.parseInt(pathInfo.substring(1));

            // Récupération de la course existante
            Course courseCourante = courseService.getCourseById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course non trouvée avec l'ID " + courseId));

            // Vérification que l'utilisateur est l'organisateur de la course ou un admin
            Participant utilisateurCourant = authService.getParticipantFromToken(token);
            if (!isAdmin && (courseCourante.getOrganisateur() == null || !courseCourante.getOrganisateur().equals(utilisateurCourant))) {

                System.out.println("Utilisateur courant : " + utilisateurCourant);
                System.out.println("Organisateur de la course : " + courseCourante.getOrganisateur());

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Vous n'êtes pas l'organisateur de cette course\"}");
                return;
            }

            // Mise à jour de tous les champs modifiables
            String nom = request.getParameter("nom");
            if (nom != null && !nom.trim().isEmpty()) {
                courseCourante.setNom(nom);
            }

            String description = request.getParameter("description");
            if (description != null) {
                courseCourante.setDescription(description);
            }

            String dateDepartStr = request.getParameter("dateDepart");
            if (dateDepartStr != null && !dateDepartStr.trim().isEmpty()) {
                Timestamp dateDepart = Timestamp.valueOf(dateDepartStr + " 00:00:00");
                courseCourante.setDateDepart(dateDepart);
            }

            String ville = request.getParameter("ville");
            if (ville != null && !ville.trim().isEmpty()) {
                courseCourante.setVille(ville);
            }

            String codePostalStr = request.getParameter("codePostal");
            if (codePostalStr != null && !codePostalStr.trim().isEmpty()) {
                int codePostal = Integer.parseInt(codePostalStr);
                courseCourante.setCodePostal(codePostal);
            }

            String adresse = request.getParameter("adresse");
            if (adresse != null) {
                courseCourante.setAdresse(adresse);
            }

            String distanceStr = request.getParameter("distance");
            if (distanceStr != null && !distanceStr.trim().isEmpty()) {
                float distance = Float.parseFloat(distanceStr);
                courseCourante.setDistance(distance);
            }

            String maxParticipantsStr = request.getParameter("maxParticipants");
            if (maxParticipantsStr != null && !maxParticipantsStr.trim().isEmpty()) {
                int maxParticipants = Integer.parseInt(maxParticipantsStr);
                courseCourante.setMaxParticipants(maxParticipants);
            }

            String prixParticipationStr = request.getParameter("prixParticipation");
            if (prixParticipationStr != null && !prixParticipationStr.trim().isEmpty()) {
                float prixParticipation = Float.parseFloat(prixParticipationStr);
                courseCourante.setPrixParticipation(prixParticipation);
            }

            String obstacles = request.getParameter("obstacles");
            if (obstacles != null) {
                courseCourante.setObstacles(obstacles);
            }

            String idCauseStr = request.getParameter("idCause");
            if (idCauseStr != null && !idCauseStr.trim().isEmpty()) {
                int idCause = Integer.parseInt(idCauseStr);
                try {
                    Cause cause = causeService.getCauseById(idCause)
                            .orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + idCause));
                    courseCourante.setCause(cause);
                } catch (Exception e) {
                    throw new ServletException("Impossible de récupérer la cause associée : " + e.getMessage());
                }
            }

            // Enregistrement des modifications
            courseService.updateCourse(courseCourante);

            // Réponse
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"message\": \"Course mise à jour avec succès\"}");

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
        boolean isOrganisateur = authService.isOrganisateur(token);
        boolean isAdmin = authService.isAdmin(token);

        if (!isOrganisateur && !isAdmin) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Permissions insuffisantes pour supprimer une course\"}");
            return;
        }

        try {
            // Extraction de l'ID de course à partir de l'URL
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.startsWith("/")) {
                throw new IllegalArgumentException("L'ID de la course doit être spécifié dans l'URL");
            }

            int courseId = Integer.parseInt(pathInfo.substring(1));

            // Récupération de la course existante pour vérifier les permissions
            Course courseCourante = courseService.getCourseById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course non trouvée avec l'ID " + courseId));

            // Vérification que l'utilisateur est l'organisateur de la course ou un admin
            if (!isAdmin) {
                Participant utilisateurCourant = authService.getParticipantFromToken(token);
                if (courseCourante.getOrganisateur() == null || !courseCourante.getOrganisateur().equals(utilisateurCourant)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\": \"Vous n'êtes pas l'organisateur de cette course\"}");
                    return;
                }
            }

            // Suppression de la course
            boolean supprime = courseService.deleteCourse(courseId);

            if (supprime) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\": \"Course supprimée avec succès\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Échec de la suppression de la course\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"ID de course invalide\"}");
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

