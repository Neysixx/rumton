package fr.esgi.color_run.servlet;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
import org.thymeleaf.context.Context;

/**
 * Servlet pour gérer les courses
 */
@WebServlet(name = "courseServlet", value = {"/courses", "/courses/*", "/courses-create"})
public class CourseServlet extends BaseWebServlet {
    private CourseService courseService;
    private CauseService causeService;

    @Override
    public void init() {
        super.init();
        courseService = new CourseServiceImpl();
        causeService = new CauseServiceImpl();
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
        if (!isAuthenticated(request, response)) {
            return;
        }

        Context context = new Context();
        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();

        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                int courseId = Integer.parseInt(pathInfo.substring(1));
                courseService.getCourseById(courseId)
                        .ifPresentOrElse(
                                course -> {
                                    try {
                                        context.setVariable("course", course);
                                        context.setVariable("isAdmin", request.getAttribute("is_admin"));
                                        context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));
                                        renderTemplate(request, response, "courses/course_details", context);
                                    } catch (IOException | ServletException e) {
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
            return;
        }

        if (Objects.equals(servletPath, "/courses-create")) {
            context.setVariable("causes", causeService.getAllCauses());
            context.setVariable("isAdmin", request.getAttribute("is_admin"));
            context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));
            renderTemplate(request, response, "courses/createCourse", context);
            return;
        }

        if (Objects.equals(servletPath, "/courses")) {
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
            String nom = request.getParameter("nom");
            if (nom != null && !nom.trim().isEmpty()) {
                course.setNom(nom);
            }

            String description = request.getParameter("description");
            if (description != null) {
                course.setDescription(description);
            }

            String dateDepartStr = request.getParameter("dateDepart");
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

            String ville = request.getParameter("ville");
            if (ville != null && !ville.trim().isEmpty()) {
                course.setVille(ville);
            }

            String codePostalStr = request.getParameter("codePostal");
            if (codePostalStr != null && !codePostalStr.trim().isEmpty()) {
                try {
                    course.setCodePostal(Integer.parseInt(codePostalStr));
                } catch (NumberFormatException e) {
                    renderError(request, response, "Le code postal doit être un nombre");
                    return;
                }
            }

            String adresse = request.getParameter("adresse");
            if (adresse != null) {
                course.setAdresse(adresse);
            }

            String distanceStr = request.getParameter("distance");
            if (distanceStr != null && !distanceStr.trim().isEmpty()) {
                try {
                    course.setDistance(Float.parseFloat(distanceStr));
                } catch (NumberFormatException e) {
                    renderError(request, response, "La distance doit être un nombre");
                    return;
                }
            }

            String maxParticipantsStr = request.getParameter("maxParticipants");
            if (maxParticipantsStr != null && !maxParticipantsStr.trim().isEmpty()) {
                try {
                    course.setMaxParticipants(Integer.parseInt(maxParticipantsStr));
                } catch (NumberFormatException e) {
                    renderError(request, response, "Le nombre maximum de participants doit être un nombre entier");
                    return;
                }
            }

            String prixParticipationStr = request.getParameter("prixParticipation");
            if (prixParticipationStr != null && !prixParticipationStr.trim().isEmpty()) {
                try {
                    course.setPrixParticipation(Float.parseFloat(prixParticipationStr));
                } catch (NumberFormatException e) {
                    renderError(request, response, "Le prix de participation doit être un nombre");
                    return;
                }
            }

            String obstacles = request.getParameter("obstacles");
            if (obstacles != null) {
                course.setObstacles(obstacles);
            }

            String idCauseStr = request.getParameter("idCause");
            if (idCauseStr != null && !idCauseStr.trim().isEmpty()) {
                try {
                    int idCause = Integer.parseInt(idCauseStr);
                    Cause cause = causeService.getCauseById(idCause)
                            .orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + idCause));
                    course.setCause(cause);
                } catch (NumberFormatException e) {
                    renderError(request, response, "L'ID de la cause doit être un nombre entier");
                    return;
                }
            }

            // Sauvegarde des modifications
            courseService.updateCourse(course);

            // Redirection vers la page de détails de la course
            response.sendRedirect(request.getContextPath() + "/courses/" + courseId);

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

