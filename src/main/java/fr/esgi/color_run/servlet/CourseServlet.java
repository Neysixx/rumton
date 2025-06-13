package fr.esgi.color_run.servlet;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Participation;
import fr.esgi.color_run.service.*;
import fr.esgi.color_run.service.impl.*;
import fr.esgi.color_run.util.DateUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.thymeleaf.context.Context;

/**
 * Servlet pour gérer les courses
 */
@WebServlet(name = "courseServlet", value = {"/courses", "/courses/*", "/courses-create", "/courses-edit/*"})
public class CourseServlet extends BaseWebServlet {
    private CourseService courseService;
    private CauseService causeService;
    private ParticipationService participationService;
    private MessageService messageService;

    @Override
    public void init() {
        super.init();
        courseService = new CourseServiceImpl();
        causeService = new CauseServiceImpl();
        participationService = new ParticipationServiceImpl();
        messageService = new MessageServiceImpl();
    }

    /**
     * Cette méthode est appelée lorsqu'une requête GET est envoyée vers le servlet
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            // faire redirection
        }

        Context context = new Context();

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
                                        Boolean isOrga = Boolean.parseBoolean(request.getAttribute("is_organisateur").toString());
                                        Boolean isAdmin = Boolean.parseBoolean(request.getAttribute("is_admin").toString());
                                        context.setVariable("course", course);
                                        context.setVariable("isAdmin", isAdmin);
                                        context.setVariable("isOrganisateur", isOrga);
                                        if (isOrga || isAdmin){
                                            // on met isInsrit à true, comme ça le bouton pour participer ne sera pas visible pour eux
                                            context.setVariable("isInscrit", true);
                                        }
                                        else {
                                            context.setVariable("isInscrit", participationService.isParticipantRegistered(getAuthenticatedParticipant(request).getIdParticipant(), courseId));

                                        }
                                        context.setVariable("participations", participationService.getParticipationsByCourse(courseId));


                                        if(Objects.equals(request.getServletPath(), "/courses")){
                                            context.setVariable("messages", messageService.getMessagesByCourse(courseId));
                                            context.setVariable("numberParticipations", participationService.getParticipationsByCourse(courseId));
                                            renderTemplate(request, response, "courses/course_details", context);
                                        } else {
                                            if(!isOrga){
                                                renderError(request, response, "Vous ne pouvez pas modifier une course si vous n'êtes pas organisateur");
                                                return;
                                            }
                                            int userId = Integer.parseInt(request.getAttribute("user_id").toString());
                                            if(userId != course.getOrganisateur().getIdParticipant()){
                                                renderError(request, response, "Vous ne pouvez pas modifier une course qui ne vous appartiens pas");
                                                return;
                                            }
                                            course.setDateDepartFormatted(DateUtil.formatDateForDatetimeLocalInput(course.getDateDepart()));
                                            context.setVariable("course", course);
                                            context.setVariable("causes", causeService.getAllCauses());
                                            renderTemplate(request, response, "courses/course_edit", context);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (ServletException e) {
                                        e.printStackTrace();
                                    }
                                },
                                () -> {
                                    try {
                                        renderError(request, response, "Course non trouvée avec l'ID " + courseId);
                                    } catch (IOException | ServletException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
            } catch (NumberFormatException e) {
                renderError(request, response, "ID de course invalide");
            }
        } else {
            if(Objects.equals(request.getServletPath(), "/courses")){
                // Ajout des courses à la vue
                Boolean isOrga = Boolean.parseBoolean(request.getAttribute("is_organisateur").toString());
                if(isOrga){
                    int userId = Integer.parseInt(request.getAttribute("user_id").toString());
                    context.setVariable("courses", courseService.getCoursesByOrgaId(userId));
                }else{
                    context.setVariable("courses", courseService.getAllCourses());
                }
                context.setVariable("isAdmin", request.getAttribute("is_admin"));
                context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));

                // Rendu de la page
                renderTemplate(request, response, "courses/list", context);
            }
            else if (Objects.equals(request.getServletPath(), "/courses-create")) {
                // create course
                Boolean isOrga = Boolean.parseBoolean(request.getAttribute("is_organisateur").toString());
                if(!isOrga){
                    renderError(request, response, "Vous ne pouvez pas créer de course si vous n'êtes pas organisateur");
                }
                // récupération des causes
                context.setVariable("causes", causeService.getAllCauses());
                context.setVariable("isAdmin", request.getAttribute("is_admin"));
                context.setVariable("isOrganisateur", isOrga);

                renderTemplate(request, response, "courses/createCourse", context);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // Vérification que l'utilisateur est un organisateur
        if (!isOrganisateur(request, response)) {
            return;
        }

        try {
            // Récupération de l'organisateur à partir du token
            Participant organisateur = getAuthenticatedParticipant(request);
            if (organisateur == null) {
                renderError(request, response, "Impossible de récupérer les informations de l'organisateur");
                return;
            }

            // Récupération des paramètres avec validation
            String nom = request.getParameter("nom");
            if (nom == null || nom.trim().isEmpty()) {
                renderError(request, response, "Le nom de la course est obligatoire");
                return;
            }
            
            String description = request.getParameter("description");
            // Description peut être null ou vide
            
            String dateDepartStr = request.getParameter("dateDepart");
            String ville = request.getParameter("ville");
            if (ville == null || ville.trim().isEmpty()) {
                renderError(request, response, "La ville est obligatoire");
                return;
            }
            
            String codePostalStr = request.getParameter("codePostal");
            if (codePostalStr == null || codePostalStr.trim().isEmpty()) {
                renderError(request, response, "Le code postal est obligatoire");
                return;
            }
            
            String adresse = request.getParameter("adresse");
            // Adresse peut être null ou vide
            
            String distanceStr = request.getParameter("distance");
            if (distanceStr == null || distanceStr.trim().isEmpty()) {
                renderError(request, response, "La distance est obligatoire");
                return;
            }
            
            String maxParticipantsStr = request.getParameter("maxParticipants");
            if (maxParticipantsStr == null || maxParticipantsStr.trim().isEmpty()) {
                renderError(request, response, "Le nombre maximum de participants est obligatoire");
                return;
            }
            
            String prixParticipationStr = request.getParameter("prixParticipation");
            if (prixParticipationStr == null || prixParticipationStr.trim().isEmpty()) {
                renderError(request, response, "Le prix de participation est obligatoire");
                return;
            }
            
            String obstacles = request.getParameter("obstacles");
            // Obstacles true ou false
            Boolean isObstacles = Objects.equals(obstacles, "on");
            
            String idCauseStr = request.getParameter("idCause");

            // Conversions avec gestion des erreurs
            java.sql.Timestamp dateDepart = new java.sql.Timestamp(System.currentTimeMillis());
            if (dateDepartStr != null && !dateDepartStr.trim().isEmpty()) {
                try {
                    java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                    java.util.Date parsedDate = inputFormat.parse(dateDepartStr);
                    dateDepart = new java.sql.Timestamp(parsedDate.getTime());
                } catch (java.text.ParseException e) {
                    renderError(request, response, "Format de date de départ invalide. Utilisez le format yyyy-MM-ddTHH:mm");
                    return;
                }
            }
            
            int codePostal;
            try {
                codePostal = Integer.parseInt(codePostalStr);
            } catch (NumberFormatException e) {
                renderError(request, response, "Le code postal doit être un nombre");
                return;
            }
            
            float distance;
            try {
                distance = Float.parseFloat(distanceStr);
            } catch (NumberFormatException e) {
                renderError(request, response, "La distance doit être un nombre");
                return;
            }
            
            int maxParticipants;
            try {
                maxParticipants = Integer.parseInt(maxParticipantsStr);
            } catch (NumberFormatException e) {
                renderError(request, response, "Le nombre maximum de participants doit être un nombre entier");
                return;
            }
            
            float prixParticipation;
            try {
                prixParticipation = Float.parseFloat(prixParticipationStr);
            } catch (NumberFormatException e) {
                renderError(request, response, "Le prix de participation doit être un nombre");
                return;
            }


            Cause cause = null;
            if(idCauseStr != null){
                int idCause;
                try {
                    idCause = Integer.parseInt(idCauseStr);
                } catch (NumberFormatException e) {
                    renderError(request, response, "L'ID de la cause doit être un nombre entier");
                    return;
                }

                // Recherche de la cause
                try {
                    cause = causeService.getCauseById(idCause)
                            .orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + idCause));
                } catch (Exception e) {
                    renderError(request, response, "Impossible de récupérer la cause associée : " + e.getMessage());
                    return;
                }

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
                    .obstacles(isObstacles.toString())
                    .cause(cause)
                    .organisateur(organisateur)
                    .build();

            // Sauvegarde de la course
            courseService.createCourse(course);

            // Redirection vers la liste des courses
            response.sendRedirect(request.getContextPath() + "/courses");

        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue lors de la création de la course : " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // Vérification que l'utilisateur est un organisateur
        if (!isOrganisateur(request, response)) {
            return;
        }

        try {
            BufferedReader reader = request.getReader();
            String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            Map<String, String> params = parseUrlEncodedBody(body);
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                renderError(request, response, "ID de course manquant");
                return;
            }

            int courseId = Integer.parseInt(pathInfo.substring(1));
            Course course = courseService.getCourseById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course non trouvée avec l'ID " + courseId));

            // Vérification que l'utilisateur est l'organisateur de la course
            Participant currentUser = getAuthenticatedParticipant(request);
            if (course.getOrganisateur().getIdParticipant() != currentUser.getIdParticipant()) {
                renderError(request, response, "Vous n'êtes pas autorisé à modifier cette course");
                return;
            }

            // Mise à jour des champs
            String nom = params.get("nom");
            if (nom != null && !nom.trim().isEmpty()) {
                course.setNom(nom);
            }

            String description = params.get("description");
            if (description != null) {
                course.setDescription(description);
            }

            String dateDepartStr = params.get("dateDepart");
            if (dateDepartStr != null && !dateDepartStr.trim().isEmpty()) {
                try {
                    java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                    java.util.Date parsedDate = inputFormat.parse(dateDepartStr);
                    course.setDateDepart(new java.sql.Timestamp(parsedDate.getTime()));
                } catch (java.text.ParseException e) {
                    renderError(request, response, "Format de date de départ invalide");
                    return;
                }
            }

            String ville = params.get("ville");
            if (ville != null && !ville.trim().isEmpty()) {
                course.setVille(ville);
            }

            String codePostalStr = params.get("codePostal");
            if (codePostalStr != null && !codePostalStr.trim().isEmpty()) {
                try {
                    course.setCodePostal(Integer.parseInt(codePostalStr));
                } catch (NumberFormatException e) {
                    renderError(request, response, "Le code postal doit être un nombre");
                    return;
                }
            }

            String adresse = params.get("adresse");
            if (adresse != null) {
                course.setAdresse(adresse);
            }

            String distanceStr = params.get("distance");
            if (distanceStr != null && !distanceStr.trim().isEmpty()) {
                try {
                    course.setDistance(Float.parseFloat(distanceStr));
                } catch (NumberFormatException e) {
                    renderError(request, response, "La distance doit être un nombre");
                    return;
                }
            }

            String maxParticipantsStr = params.get("maxParticipants");
            if (maxParticipantsStr != null && !maxParticipantsStr.trim().isEmpty()) {
                try {
                    course.setMaxParticipants(Integer.parseInt(maxParticipantsStr));
                } catch (NumberFormatException e) {
                    renderError(request, response, "Le nombre maximum de participants doit être un nombre entier");
                    return;
                }
            }

            String prixParticipationStr = params.get("prixParticipation");
            if (prixParticipationStr != null && !prixParticipationStr.trim().isEmpty()) {
                try {
                    course.setPrixParticipation(Float.parseFloat(prixParticipationStr));
                } catch (NumberFormatException e) {
                    renderError(request, response, "Le prix de participation doit être un nombre");
                    return;
                }
            }

            String obstacles = params.get("obstacles");
            if (obstacles != null) {
                course.setObstacles(obstacles);
            }

            String idCauseStr = params.get("idCause");
            Cause cause = null;
            if(idCauseStr != null){
                int idCause;
                try {
                    idCause = Integer.parseInt(idCauseStr);
                } catch (NumberFormatException e) {
                    renderError(request, response, "L'ID de la cause doit être un nombre entier");
                    return;
                }

                // Recherche de la cause
                try {
                    cause = causeService.getCauseById(idCause)
                            .orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + idCause));
                } catch (Exception e) {
                    renderError(request, response, "Impossible de récupérer la cause associée : " + e.getMessage());
                    return;
                }

            }

            // Sauvegarde des modifications
            courseService.updateCourse(course);
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de course invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue lors de la mise à jour de la course : " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // Vérification que l'utilisateur est un organisateur
        if (!isOrganisateur(request, response)) {
            return;
        }

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                renderError(request, response, "ID de course manquant");
                return;
            }

            int courseId = Integer.parseInt(pathInfo.substring(1));
            Course course = courseService.getCourseById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course non trouvée avec l'ID " + courseId));

            // Vérification que l'utilisateur est l'organisateur de la course
            Participant currentUser = getAuthenticatedParticipant(request);
            if (course.getOrganisateur().getIdParticipant() != currentUser.getIdParticipant()) {
                renderError(request, response, "Vous n'êtes pas autorisé à supprimer cette course");
                return;
            }

            // Suppression de la course
            courseService.deleteCourse(courseId);

            // Redirection vers la liste des courses
            response.sendRedirect(request.getContextPath() + "/courses");

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de course invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue lors de la suppression de la course : " + e.getMessage());
        }
    }
}

