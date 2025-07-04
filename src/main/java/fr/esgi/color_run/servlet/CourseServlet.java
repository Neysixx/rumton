package fr.esgi.color_run.servlet;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Participant;
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
@WebServlet(name = "courseServlet", value = { "/courses", "/courses/*", "/courses-create", "/courses-edit/*", "/courses-delete/*" })
public class CourseServlet extends BaseWebServlet {
    private CourseService courseService;
    private CauseService causeService;
    private ParticipationService participationService;
    private MessageService messageService;
    private StripeService stripeService;

    @Override
    public void init() {
        super.init();
        courseService = new CourseServiceImpl();
        causeService = new CauseServiceImpl();
        participationService = new ParticipationServiceImpl();
        messageService = new MessageServiceImpl();
        stripeService = new StripeServiceImpl();
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
                                            if(isInscrit) {
                                                // id de la participation pour télécharger le dossard
                                                int participationId = participationService.getParticipationIdByCourseAndParticipant(courseId, getAuthenticatedParticipant(request).getIdParticipant());
                                                context.setVariable("participationId", participationId);
                                            }
                                            context.setVariable("isInscrit", isInscrit);
                                            context.setVariable("canParticipate", !isInscrit);
                                        } else {
                                            // Utilisateurs non authentifiés
                                            context.setVariable("isInscrit", false);
                                            context.setVariable("canParticipate", false);
                                        }

                                        context.setVariable("participations",
                                                participationService.getParticipationsByCourse(courseId));

                                        if (Objects.equals(request.getServletPath(), "/courses")) {
                                            context.setVariable("messages",
                                                    messageService.getMessagesByCourse(courseId));
                                            context.setVariable("numberParticipations",
                                                    participationService.getParticipationsByCourse(courseId));
                                            renderTemplate(request, response, "courses/course_details", context);
                                        } else if (Objects.equals(request.getServletPath(), "/courses-edit")) {
                                            // Page d'édition - vérifications pour organisateurs et admins
                                            if (!isOrganisateur && !isAdmin) {
                                                renderError(request, response,
                                                        "Vous ne pouvez pas modifier une course si vous n'êtes pas organisateur ou administrateur");
                                                return;
                                            }
                                            int userId = Integer.parseInt(request.getAttribute("user_id").toString());
                                            if (userId != course.getOrganisateur().getIdParticipant() && !isAdmin) {
                                                renderError(request, response,
                                                        "Vous ne pouvez pas modifier une course qui ne vous appartient pas");
                                                return;
                                            }
                                            course.setDateDepartFormatted(
                                                    DateUtil.formatDateForDatetimeLocalInput(course.getDateDepart()));
                                            context.setVariable("course", course);
                                            context.setVariable("causes", causeService.getAllCauses());
                                            renderTemplate(request, response, "courses/course_edit", context);
                                        } else if (Objects.equals(request.getServletPath(), "/courses-delete")) {
                                            // Page de confirmation de suppression - vérifications pour organisateurs et
                                            // admins
                                            if (!isOrganisateur && !isAdmin) {
                                                renderError(request, response,
                                                        "Vous ne pouvez pas supprimer une course si vous n'êtes pas organisateur ou administrateur");
                                                return;
                                            }
                                            int userId = Integer.parseInt(request.getAttribute("user_id").toString());
                                            if (userId != course.getOrganisateur().getIdParticipant() && !isAdmin) {
                                                renderError(request, response,
                                                        "Vous ne pouvez pas supprimer une course qui ne vous appartient pas");
                                                return;
                                            }
                                            context.setVariable("course", course);
                                            renderTemplate(request, response, "courses/course_delete_confirm", context);
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
                                });
            } catch (NumberFormatException e) {
                renderError(request, response, "ID de course invalide");
            }
        } else {
            try {
                if (Objects.equals(request.getServletPath(), "/courses")) {
                    // Logique d'affichage de la liste selon les rôles
                    if (isAuthenticated && isOrganisateur) {
                        // Organisateur : voit uniquement ses propres courses
                        int userId = Integer.parseInt(request.getAttribute("user_id").toString());
                        context.setVariable("courses", courseService.getCoursesByOrgaId(userId));
                    } else {
                        // Admin et participants : voient toutes les courses
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

                    // Si l'utilisateur est authentifié et n'est pas organisateur/admin, 
                    // vérifier ses inscriptions pour chaque course
                    if (isAuthenticated && !isOrganisateur && !isAdmin) {
                        int participantId = getAuthenticatedParticipant(request).getIdParticipant();
                        Map<Integer, Boolean> inscriptionsMap = new HashMap<>();
                        for (Course course : filteredCourses) {
                            boolean isInscrit = participationService.isParticipantRegistered(participantId, course.getIdCourse());
                            inscriptionsMap.put(course.getIdCourse(), isInscrit);
                        }
                        context.setVariable("inscriptions", inscriptionsMap);
                    }

                    context.setVariable("courses", filteredCourses);
                    context.setVariable("isAdmin", request.getAttribute("is_admin"));
                    context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));

                    // Filtrer les villes des courses futures uniquement
                    Date now = new Date();
                    List<String> villes = courseService.getAllCourses().stream()
                            .filter(c -> c.getDateDepart().after(now)) // Seulement les courses futures
                            .map(Course::getVille)
                            .filter(Objects::nonNull)
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList());

                    context.setVariable("villes", villes);
                    renderTemplate(request, response, "courses/list", context);
                }else if (Objects.equals(request.getServletPath(), "/courses-create")) {
                    // Création de course - uniquement pour organisateurs
                    if (!isOrganisateur) {
                        renderError(request, response,
                                "Vous ne pouvez pas créer de course si vous n'êtes pas organisateur");
                        return;
                    }

                    context.setVariable("causes", causeService.getAllCauses());
                    renderTemplate(request, response, "courses/create_course", context);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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
                    renderError(request, response,
                            "Format de date de départ invalide. Utilisez le format yyyy-MM-ddTHH:mm");
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
            if (latitudeStr != null) {
                try {
                    latitude = Float.parseFloat(latitudeStr);
                } catch (NumberFormatException e) {
                    latitude = null;
                }
            } else {
                latitude = null;
            }

            Float longitude;
            if (latitudeStr != null) {
                try {
                    longitude = Float.parseFloat(longitudeStr);
                } catch (NumberFormatException e) {
                    longitude = null;
                }
            } else {
                longitude = null;
            }

            Cause cause = null;
            if (idCauseStr != null) {
                int idCause;
                try {
                    idCause = Integer.parseInt(idCauseStr);
                } catch (NumberFormatException e) {
                    renderError(request, response, "L'ID de la cause doit être un nombre entier");
                    return;
                }

                // Recherche de la cause
                try {
                    cause = causeService.getCauseById(idCause).orElseThrow(() -> new IllegalArgumentException("Cause non trouvée avec l'ID " + idCause));
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

            // Sauvegarde de la course d'abord pour obtenir l'ID
            courseService.createCourse(course);
            
            // Créer le produit Stripe si le prix est supérieur à 0
            if (prixParticipation > 0) {
                // Récupérer la course créée avec son ID
                final java.sql.Timestamp finalDateDepart = dateDepart;
                List<Course> userCourses = courseService.getCoursesByOrgaId(organisateur.getIdParticipant());
                Course createdCourse = userCourses.stream()
                    .filter(c -> c.getNom().equals(nom) && c.getDateDepart().equals(finalDateDepart))
                    .findFirst()
                    .orElse(null);
                
                if (createdCourse != null) {
                    String stripeProductId = stripeService.createProductForCourse(createdCourse);
                    if (stripeProductId != null) {
                        createdCourse.setStripeProductId(stripeProductId);
                        courseService.updateCourse(createdCourse);
                        System.out.println("Produit Stripe créé et associé à la course: " + stripeProductId);
                    } else {
                        System.err.println("Erreur lors de la création du produit Stripe pour la course: " + createdCourse.getNom());
                    }
                }
            }

            // Redirection vers la liste des courses
            response.sendRedirect(request.getContextPath() + "/courses");

        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response,
                    "Une erreur est survenue lors de la création de la course : " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // Vérification que l'utilisateur est un organisateur
        if (!isOrganisateur(request, response) && !isAdmin(request, response)) {
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

            // Vérification que l'utilisateur est l'organisateur de la course ou un admin
            boolean isAdminUser = isAdmin(request, response);
            if (!isAdminUser) {
                // Si ce n'est pas un admin, vérifier que c'est l'organisateur de la course
                Participant currentUser = getAuthenticatedParticipant(request);
                if (currentUser == null || course.getOrganisateur().getIdParticipant() != currentUser.getIdParticipant()) {
                    renderError(request, response, "Vous n'êtes pas autorisé à modifier cette course");
                    return;
                }
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
            float oldPrice = course.getPrixParticipation();
            boolean priceChanged = false;
            if (prixParticipationStr != null && !prixParticipationStr.trim().isEmpty()) {
                try {
                    float newPrice = Float.parseFloat(prixParticipationStr);
                    if (Math.abs(oldPrice - newPrice) > 0.01f) { // Considérer comme changé si différence > 1 centime
                        priceChanged = true;
                    }
                    course.setPrixParticipation(newPrice);
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
            if (idCauseStr != null) {
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
            if (cause != null) {
                course.setCause(cause);
            }
            
            // Mettre à jour le produit Stripe selon les changements
            if (course.getPrixParticipation() > 0) {
                if (course.getStripeProductId() != null) {
                    // Mise à jour du produit existant
                    boolean updateSuccess;
                    if (priceChanged) {
                        // Le prix a changé, utiliser la méthode spéciale qui gère les nouveaux prix
                        updateSuccess = stripeService.updateProductWithPrice(course.getStripeProductId(), course, course.getPrixParticipation());
                        if (updateSuccess) {
                            System.out.println("Produit Stripe mis à jour avec nouveau prix: " + course.getStripeProductId());
                        } else {
                            System.err.println("Erreur lors de la mise à jour du produit Stripe avec nouveau prix: " + course.getStripeProductId());
                        }
                    } else {
                        // Juste mettre à jour les infos du produit (nom, description, etc.)
                        updateSuccess = stripeService.updateProduct(course.getStripeProductId(), course);
                        if (updateSuccess) {
                            System.out.println("Produit Stripe mis à jour: " + course.getStripeProductId());
                        } else {
                            System.err.println("Erreur lors de la mise à jour du produit Stripe: " + course.getStripeProductId());
                        }
                    }
                } else {
                    // Créer un nouveau produit si le prix devient payant
                    String stripeProductId = stripeService.createProductForCourse(course);
                    if (stripeProductId != null) {
                        course.setStripeProductId(stripeProductId);
                        System.out.println("Nouveau produit Stripe créé: " + stripeProductId);
                    }
                }
            } else if (course.getStripeProductId() != null) {
                // Désactiver le produit si le prix devient gratuit
                boolean deleteSuccess = stripeService.deleteProduct(course.getStripeProductId());
                if (deleteSuccess) {
                    course.setStripeProductId(null);
                    System.out.println("Produit Stripe désactivé car course gratuite");
                }
            }
            
            // Sauvegarde des modifications
            courseService.updateCourse(course);
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de course invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response,
                    "Une erreur est survenue lors de la mise à jour de la course : " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        // Vérification que l'utilisateur est un organisateur
        if (!isOrganisateur(request, response) && !isAdmin(request, response)) {
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

            // Vérification que l'utilisateur est l'organisateur de la course ou un admin
            boolean isAdminUser = isAdmin(request, response);
            if (!isAdminUser) {
                // Si ce n'est pas un admin, vérifier que c'est l'organisateur de la course
                Participant currentUser = getAuthenticatedParticipant(request);
                if (currentUser == null || course.getOrganisateur().getIdParticipant() != currentUser.getIdParticipant()) {
                    renderError(request, response, "Vous n'êtes pas autorisé à supprimer cette course");
                    return;
                }
            }

            // Supprimer le produit Stripe associé s'il existe
            if (course.getStripeProductId() != null) {
                boolean deleteSuccess = stripeService.deleteProduct(course.getStripeProductId());
                if (deleteSuccess) {
                    System.out.println("Produit Stripe supprimé: " + course.getStripeProductId());
                } else {
                    System.err.println("Erreur lors de la suppression du produit Stripe: " + course.getStripeProductId());
                }
            }

            // Suppression de la course
            courseService.deleteCourse(courseId);

            // Redirection vers la liste des courses
            response.sendRedirect(request.getContextPath() + "/courses");

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de course invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response,
                    "Une erreur est survenue lors de la suppression de la course : " + e.getMessage());
        }
    }
}
