package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Participation;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.ParticipationService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import fr.esgi.color_run.service.impl.ParticipationServiceImpl;
import fr.esgi.color_run.util.DebugUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Servlet pour gérer les participations aux courses
 */
@WebServlet(name = "participationServlet", value = { "/participations", "/participations/*" })
public class ParticipationServlet extends HttpServlet {

    private AuthService authService;
    private ParticipationService participationService;
    private CourseService courseService;

    @Override
    public void init() {
        authService = new AuthServiceImpl();
        participationService = new ParticipationServiceImpl();
        courseService = new CourseServiceImpl();
    }

    /**
     * Traite les requêtes GET pour afficher toutes les participations ou une
     * participation spécifique
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Récupération du token depuis les attributs de la requête (placé par le
        // filtre)
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        // On récupère le moteur de template dans le contexte des servlets
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        Context context = new Context();

        // Configuration de la réponse en JSON pour les requêtes d'API
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Vérification du chemin pour déterminer s'il s'agit d'une demande de
        // participation spécifique
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                // Extraction de l'ID de la participation à partir de l'URL
                // (/participations/{id})
                int participationId = Integer.parseInt(pathInfo.substring(1));

                // Récupérer une participation spécifique
                participationService.getParticipationById(participationId)
                        .ifPresentOrElse(
                                participation -> {
                                    try {
                                        // Vérification que l'utilisateur est autorisé à voir cette participation
                                        Participant currentUser = authService.getParticipantFromToken(token);
                                        if (currentUser == null ||
                                                (!currentUser.equals(participation.getParticipant()) &&
                                                        !authService.isAdmin(token) &&
                                                        !isOrganisateurOfCourse(currentUser,
                                                                participation.getCourse()))) {
                                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                            response.getWriter().write(
                                                    "{\"error\": \"Vous n'avez pas accès à cette participation\"}");
                                            return;
                                        }

                                        response.setStatus(HttpServletResponse.SC_OK);
                                        response.getWriter().write(participation.toString());

                                        // Pour les vues HTML
                                        context.setVariable("participation", participation);
                                        templateEngine.process("participation", context, response.getWriter());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                },
                                () -> {
                                    try {
                                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                        response.getWriter().write("{\"error\": \"Participation non trouvée avec l'ID "
                                                + participationId + "\"}");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"ID de participation invalide\"}");
            }
        } else {
            // Si l'utilisateur est un admin, afficher toutes les participations
            if (authService.isAdmin(token)) {
                List<Participation> participations = participationService.getAllParticipations();
                context.setVariable("participations", participations);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(participations.toString());
            } else {
                // Sinon, afficher uniquement les participations de l'utilisateur
                Participant currentUser = authService.getParticipantFromToken(token);
                if (currentUser != null) {
                    List<Participation> userParticipations = participationService
                            .getParticipationsByParticipant(currentUser.getIdParticipant());
                    context.setVariable("participations", userParticipations);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(userParticipations.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\": \"Accès refusé\"}");
                }
            }
            templateEngine.process("participations", context, response.getWriter());
        }
    }

    /**
     * Traite les requêtes POST pour créer une nouvelle participation
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Vérification de l'authentification
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            // Récupération de l'utilisateur courant
            Participant participant = authService.getParticipantFromToken(token);
            if (participant == null) {
                throw new ServletException("Impossible de récupérer les informations du participant");
            }

            // Récupération des paramètres
            String courseIdStr = request.getParameter("idCourse");
            if (courseIdStr == null || courseIdStr.trim().isEmpty()) {
                throw new IllegalArgumentException("L'ID de la course est obligatoire");
            }

            int courseId = Integer.parseInt(courseIdStr);

            // Récupérer la course
            Optional<Course> optCourse = courseService.getCourseById(courseId);
            if (optCourse.isEmpty()) {
                throw new IllegalArgumentException("Course non trouvée avec l'ID " + courseId);
            }

            Course course = optCourse.get();

            // Vérifier si la course n'est pas déjà complète
            int currentParticipations = participationService.getCountForCourse(courseId);
            if (currentParticipations >= course.getMaxParticipants()) {
                throw new IllegalArgumentException("Cette course est complète, plus d'inscriptions possibles");
            }

            // Vérifier si le participant n'est pas déjà inscrit
            if (participationService.isParticipantRegistered(participant.getIdParticipant(), courseId)) {
                throw new IllegalArgumentException("Vous êtes déjà inscrit à cette course");
            }

            // Génération d'un numéro de dossard unique pour cette course
            int numeroDossard = generateUniqueBibNumber(courseId);

            // Création de la participation
            Participation participation = Participation.builder()
                    .participant(participant)
                    .course(course)
                    .numeroDossard(numeroDossard)
                    .dateReservation(new Date())
                    .build();

            DebugUtil.log(this.getClass(), "Nouvelle participation créée : " + participation);

            // Enregistrement de la participation
            participationService.createParticipation(participation);

            // Réponse
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter()
                    .write("{\"message\": \"Inscription réussie\", \"numeroDossard\": " + numeroDossard + "}");

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format de données invalide: " + e.getMessage() + "\"}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Erreur lors de l'inscription: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Traite les requêtes DELETE pour supprimer une participation (annulation)
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Vérification de l'authentification
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            // Vérification du chemin
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || !pathInfo.startsWith("/")) {
                throw new IllegalArgumentException("L'ID de la participation doit être spécifié dans l'URL");
            }

            int participationId = Integer.parseInt(pathInfo.substring(1));

            // Récupération de la participation
            Optional<Participation> optParticipation = participationService.getParticipationById(participationId);
            if (optParticipation.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Participation non trouvée\"}");
                return;
            }

            Participation participation = optParticipation.get();

            // Vérification des permissions
            Participant currentUser = authService.getParticipantFromToken(token);
            boolean isAdmin = authService.isAdmin(token);

            if (!isAdmin && (currentUser == null || !currentUser.equals(participation.getParticipant()))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Vous n'êtes pas autorisé à annuler cette participation\"}");
                return;
            }

            // Suppression de la participation
            boolean deleted = participationService.deleteParticipation(participationId);

            if (deleted) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\": \"Participation annulée avec succès\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Échec de l'annulation de la participation\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"ID de participation invalide\"}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Vérifie si un participant est l'organisateur d'une course
     */
    private boolean isOrganisateurOfCourse(Participant participant, Course course) {
        if (participant == null || course == null || course.getOrganisateur() == null) {
            return false;
        }
        return course.getOrganisateur().getIdParticipant() == participant.getIdParticipant();
    }

    /**
     * Génère un numéro de dossard unique pour une course
     */
    private int generateUniqueBibNumber(int courseId) {
        // Obtenir le dernier numéro de dossard utilisé pour cette course
        int lastNumber = participationService.getLastBibNumberForCourse(courseId);

        // Incrémenter le numéro
        return lastNumber + 1;
    }
}
