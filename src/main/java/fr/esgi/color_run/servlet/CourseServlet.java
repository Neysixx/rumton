package fr.esgi.color_run.servlet;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.*;
import fr.esgi.color_run.service.impl.*;
import fr.esgi.color_run.util.DateUtil;
import fr.esgi.color_run.util.DebugUtil;
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

    private boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    private Comparator<Course> getComparator(String sort) {
        if (sort == null || sort.isEmpty()) {
            return Comparator.comparing(Course::getIdCourse);
        }

        if ("date-asc".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Course::getDateDepart);
        } else if ("date-desc".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Course::getDateDepart).reversed();
        } else if ("distance-asc".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Course::getDistance);
        } else if ("distance-desc".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Course::getDistance).reversed();
        } else if ("ville".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Course::getVille, String.CASE_INSENSITIVE_ORDER);
        }

        return Comparator.comparing(Course::getIdCourse);
    }

    /**
     * Cette méthode est appelée lorsqu'une requête GET est envoyée vers le servlet
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        Context context = new Context();

        final Boolean isAuthenticated = isAuthenticated(request, response);
        final Boolean isOrganisateur;
        final Boolean isAdmin;

        if (isAuthenticated) {
            isOrganisateur = Boolean.parseBoolean(request.getAttribute("is_organisateur").toString());
            isAdmin = Boolean.parseBoolean(request.getAttribute("is_admin").toString());
        } else {
            isOrganisateur = false;
            isAdmin = false;
        }

        context.setVariable("isAdmin", isAdmin);
        context.setVariable("isOrganisateur", isOrganisateur);

        // Vérification si nous avons un ID de course dans l'URL
        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();

        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                // Extraction de l'ID de la course à partir de l'URL (/courses/{id})
                int courseId = Integer.parseInt(pathInfo.substring(1));

                // Récupérer une course spécifique
                courseService.getCourseById(courseId)
                        .ifPresentOrElse(
                                course -> {
                                    try {
                                        context.setVariable("course", course);
                                        
                                        // Gestion selon les rôles pour l'affichage des détails
                                        if (isOrganisateur || isAdmin) {
                                            // Organisateurs et admins ne peuvent pas participer
                                            context.setVariable("isInscrit", true);
                                            context.setVariable("canParticipate", false);
                                        } else if (isAuthenticated) {
                                            // Participants peuvent participer
                                            boolean isInscrit = participationService.isParticipantRegistered(getAuthenticatedParticipant(request).getIdParticipant(), courseId);
                                            context.setVariable("isInscrit", isInscrit);
                                            context.setVariable("canParticipate", !isInscrit);
                                        } else {
                                            // Utilisateurs non authentifiés
                                            context.setVariable("isInscrit", false);
                                            context.setVariable("canParticipate", false);
                                        }
                                        
                                        context.setVariable("participations", participationService.getParticipationsByCourse(courseId));

                                        if(Objects.equals(request.getServletPath(), "/courses")){
                                            context.setVariable("messages", messageService.getMessagesByCourse(courseId));
                                            context.setVariable("numberParticipations", participationService.getParticipationsByCourse(courseId));
                                            renderTemplate(request, response, "courses/course_details", context);
                                        } else {
                                            // Page d'édition - vérifications pour organisateurs uniquement
                                            if(!isOrganisateur){
                                                renderError(request, response, "Vous ne pouvez pas modifier une course si vous n'êtes pas organisateur");
                                                return;
                                            }
                                            int userId = Integer.parseInt(request.getAttribute("user_id").toString());
                                            if(userId != course.getOrganisateur().getIdParticipant()){
                                                renderError(request, response, "Vous ne pouvez pas modifier une course qui ne vous appartient pas");
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
            if(Objects.equals(request.getServletPath(), "/courses")) {
                // Ajout des courses à la vue
                Boolean isOrga = Boolean.parseBoolean(request.getAttribute("is_organisateur").toString());
                if (isOrga) {
                    int userId = Integer.parseInt(request.getAttribute("user_id").toString());
                    context.setVariable("courses", courseService.getCoursesByOrgaId(userId));
                } else {
                    context.setVariable("courses", courseService.getAllCourses());
                }
                context.setVariable("isAdmin", request.getAttribute("is_admin"));
                context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));

                String dateStr = request.getParameter("date");
                String ville = request.getParameter("city");
                String distanceStr = request.getParameter("distance");
                String sort = request.getParameter("sort");

                if (ville != null && (ville.equals("all") || ville.equalsIgnoreCase("Toutes les villes"))) {
                    ville = "";
                }

                Date date = null;
                Float minDistance = null;
                Float maxDistance = null;

                try {
                    if (dateStr != null && !dateStr.isEmpty()) {
                        date = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
                    }
                    if (distanceStr != null && !distanceStr.isEmpty() && distanceStr.contains("-")) {
                        String[] parts = distanceStr.split("-");
                        minDistance = Float.parseFloat(parts[0]);
                        maxDistance = Float.parseFloat(parts[1]);
                    }
                } catch (ParseException | NumberFormatException e) {
                    renderError(request, response, "Paramètres de filtre invalides");
                    return;
                }
                final Date finalDate = date;
                final Float finalMinDistance = minDistance;
                final Float finalMaxDistance = maxDistance;
                final String finalVille = ville;

                List<Course> filteredCourses = courseService.getAllCourses().stream()
                        .filter(c -> finalDate == null || isSameDay(c.getDateDepart(), finalDate))
                        .filter(c -> finalVille == null || finalVille.isEmpty() || c.getVille().equalsIgnoreCase(finalVille))
                        .filter(c -> finalMinDistance == null || (c.getDistance() >= finalMinDistance && c.getDistance() <= finalMaxDistance))
                        .sorted(getComparator(sort))
                        .collect(Collectors.toList());

                System.out.println("Courses trouvées : " + filteredCourses.size());

                context.setVariable("courses", filteredCourses);
                context.setVariable("isAdmin", request.getAttribute("is_admin"));
                context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));

                List<String> villes = courseService.getAllCourses().stream()
                        .map(Course::getVille)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                context.setVariable("villes", villes);
                renderTemplate(request, response, "courses/list", context);
            }
            else if (Objects.equals(request.getServletPath(), "/courses-create")) {
                    // Création de course - uniquement pour organisateurs
                    if(!isOrganisateur){
                        renderError(request, response, "Vous ne pouvez pas créer de course si vous n'êtes pas organisateur");
                        return;
                    }
                    
                    context.setVariable("causes", causeService.getAllCauses());
                    renderTemplate(request, response, "courses/createCourse", context);
                }
            } catch (Exception e) {
                e.printStackTrace();
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

            String latitudeStr = request.getParameter("lat");
            String longitudeStr = request.getParameter("long");
            
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

            Float latitude;
            if(latitudeStr != null){
                try{
                    latitude = Float.parseFloat(latitudeStr);
                } catch (NumberFormatException e){
                    latitude = null;
                }
            } else {
                latitude = null;
            }

            Float longitude;
            if(latitudeStr != null){
                try{
                    longitude = Float.parseFloat(longitudeStr);
                } catch (NumberFormatException e){
                    longitude = null;
                }
            } else {
                longitude = null;
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
                    .lat(latitude)
                    .lon(longitude)
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
            if(cause != null){
                course.setCause(cause);
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

