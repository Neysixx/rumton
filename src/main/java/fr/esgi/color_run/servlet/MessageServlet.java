package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Message;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.AuthService;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.MessageService;
import fr.esgi.color_run.service.impl.AuthServiceImpl;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import fr.esgi.color_run.service.impl.MessageServiceImpl;
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
 * Servlet pour gérer les messages liés aux courses
 */
@WebServlet(name = "messageServlet", value = {"/courses/*/messages/*"})
public class MessageServlet extends HttpServlet {

    private AuthService authService;
    private MessageService messageService;
    private CourseService courseService;

    @Override
    public void init() {
        authService = new AuthServiceImpl();
        messageService = new MessageServiceImpl();
        courseService = new CourseServiceImpl();
    }

    /**
     * Extraire l'ID de la course à partir du chemin de l'URL
     * /courses/{courseId}/messages/ ou /courses/{courseId}/messages/{messageId}
     */
    private int extractCourseId(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        System.out.println("DEBUG - RequestURI pour extraction courseId: " + requestURI);

        // Pattern plus précis pour extraire l'ID de la course
        String[] parts = requestURI.split("/");
        // L'URL devrait être /context/courses/ID/messages/...
        for (int i = 0; i < parts.length; i++) {
            if ("courses".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    return Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("ID de course invalide: " + parts[i + 1]);
                }
            }
        }
        throw new IllegalArgumentException("Format d'URL invalide pour l'extraction de l'ID de course");
    }

    /**
     * Extraire l'ID du message à partir du chemin de l'URL
     * /courses/{courseId}/messages/{messageId}
     */
    private int extractMessageId(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        System.out.println("DEBUG - RequestURI pour extraction messageId: " + requestURI);

        // Pattern plus précis pour extraire l'ID du message
        String[] parts = requestURI.split("/");
        // L'URL devrait être /context/courses/ID/messages/ID
        for (int i = 0; i < parts.length; i++) {
            if ("messages".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    return Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("ID de message invalide: " + parts[i + 1]);
                }
            }
        }
        throw new IllegalArgumentException("Format d'URL invalide pour l'extraction de l'ID de message");
    }

    /**
     * Traite les requêtes GET pour afficher les messages
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        System.out.println("DEBUG - Token valide: " + token);
        System.out.println("DEBUG - RequestURI: " + request.getRequestURI());
        System.out.println("DEBUG - PathInfo: " + request.getPathInfo());

        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        Context context = new Context();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String requestURI = request.getRequestURI();

        try {
            // Vérifier s'il s'agit d'un message spécifique ou d'une liste de messages
            if (requestURI.matches(".*/courses/\\d+/messages/\\d+/?")) {
                // Gestion d'un message spécifique
                int courseId = extractCourseId(request);
                int messageId = extractMessageId(request);

                System.out.println("DEBUG - ID de course extrait: " + courseId);
                System.out.println("DEBUG - ID de message extrait: " + messageId);

                Optional<Course> optCourse = courseService.getCourseById(courseId);
                if (optCourse.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Course non trouvée\"}");
                    return;
                }

                Optional<Message> optMessage = messageService.getMessageById(messageId);
                if (optMessage.isEmpty() || optMessage.get().getCourse().getIdCourse() != courseId) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Message non trouvé pour cette course\"}");
                    return;
                }

                context.setVariable("message", optMessage.get());

                List<Message> replies = messageService.getRepliesByParent(messageId);
                context.setVariable("replies", replies);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(optMessage.get().toString());
                templateEngine.process("message", context, response.getWriter());
            } else if (requestURI.matches(".*/courses/\\d+/messages/?")) {
                // Gestion de la liste des messages d'une course
                int courseId = extractCourseId(request);

                System.out.println("DEBUG - ID de course extrait: " + courseId);

                Optional<Course> optCourse = courseService.getCourseById(courseId);
                if (optCourse.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Course non trouvée\"}");
                    return;
                }

                List<Message> messages = messageService.getMessagesByCourse(courseId);

                context.setVariable("messages", messages);
                context.setVariable("course", optCourse.get());

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(messages.toString());
                templateEngine.process("messages", context, response.getWriter());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Format d'URL non pris en charge: " + requestURI + "\"}");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Traite les requêtes POST pour créer un nouveau message
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            Participant participant = authService.getParticipantFromToken(token);
            if (participant == null) {
                throw new ServletException("Impossible de récupérer les informations du participant");
            }

            String contenu = request.getParameter("contenu");
            if (contenu == null || contenu.trim().isEmpty()) {
                throw new IllegalArgumentException("Le contenu du message est obligatoire");
            }

            String requestURI = request.getRequestURI();
            System.out.println("DEBUG - RequestURI POST: " + requestURI);

            if (!requestURI.matches(".*/courses/\\d+/messages/?")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Format d'URL invalide. Utilisez /courses/{courseId}/messages/\"}");
                return;
            }

            int courseId = extractCourseId(request);
            System.out.println("DEBUG - ID de course extrait pour POST: " + courseId);

            Optional<Course> optCourse = courseService.getCourseById(courseId);
            if (optCourse.isEmpty()) {
                throw new IllegalArgumentException("Course non trouvée avec l'ID " + courseId);
            }

            Course course = optCourse.get();
            Date datePublication = new Date(System.currentTimeMillis());

            Message message = Message.builder()
                    .emetteur(participant)
                    .course(course)
                    .contenu(contenu)
                    .datePublication(datePublication)
                    .build();

            String parentIdStr = request.getParameter("messageParentId");
            if (parentIdStr != null && !parentIdStr.trim().isEmpty()) {
                int parentId = Integer.parseInt(parentIdStr);
                Optional<Message> optParent = messageService.getMessageById(parentId);
                if (optParent.isPresent() && optParent.get().getCourse().getIdCourse() == courseId) {
                    message.setMessageParent(optParent.get());
                } else {
                    throw new IllegalArgumentException("Message parent invalide");
                }
            }

            messageService.createMessage(message);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"message\": \"Message publié avec succès\", \"id\": \"" + message.getIdMessage() + "\"}");

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
     * Traite les requêtes PUT pour modifier un message
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            Participant currentUser = authService.getParticipantFromToken(token);
            boolean isAdmin = authService.isAdmin(token);

            if (currentUser == null && !isAdmin) {
                throw new ServletException("Impossible de récupérer les informations de l'utilisateur");
            }

            String requestURI = request.getRequestURI();
            System.out.println("DEBUG - RequestURI PUT: " + requestURI);

            if (!requestURI.matches(".*/courses/\\d+/messages/\\d+/?")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Format d'URL invalide. Utilisez /courses/{courseId}/messages/{messageId}\"}");
                return;
            }

            int courseId = extractCourseId(request);
            int messageId = extractMessageId(request);

            System.out.println("DEBUG - ID de course extrait pour PUT: " + courseId);
            System.out.println("DEBUG - ID de message extrait pour PUT: " + messageId);

            Optional<Course> optCourse = courseService.getCourseById(courseId);
            if (optCourse.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Course non trouvée\"}");
                return;
            }

            Optional<Message> optMessage = messageService.getMessageById(messageId);
            if (optMessage.isEmpty() || optMessage.get().getCourse().getIdCourse() != courseId) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Message non trouvé pour cette course\"}");
                return;
            }

            Message message = optMessage.get();

            if (!isAdmin && (currentUser == null || message.getEmetteur().getIdParticipant() != currentUser.getIdParticipant())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Vous n'êtes pas autorisé à modifier ce message\"}");
                return;
            }

            String contenu = request.getParameter("contenu");
            if (contenu == null || contenu.trim().isEmpty()) {
                throw new IllegalArgumentException("Le contenu du message est obligatoire");
            }

            message.setContenu(contenu);
            messageService.updateMessage(message);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"message\": \"Message modifié avec succès\"}");

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
     * Traite les requêtes DELETE pour supprimer un message
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            Participant currentUser = authService.getParticipantFromToken(token);
            boolean isAdmin = authService.isAdmin(token);

            String requestURI = request.getRequestURI();
            System.out.println("DEBUG - RequestURI DELETE: " + requestURI);

            if (!requestURI.matches(".*/courses/\\d+/messages/\\d+/?")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Format d'URL invalide. Utilisez /courses/{courseId}/messages/{messageId}\"}");
                return;
            }

            int courseId = extractCourseId(request);
            int messageId = extractMessageId(request);

            System.out.println("DEBUG - ID de course extrait pour DELETE: " + courseId);
            System.out.println("DEBUG - ID de message extrait pour DELETE: " + messageId);

            Optional<Course> optCourse = courseService.getCourseById(courseId);
            if (optCourse.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Course non trouvée\"}");
                return;
            }

            Optional<Message> optMessage = messageService.getMessageById(messageId);
            if (optMessage.isEmpty() || optMessage.get().getCourse().getIdCourse() != courseId) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Message non trouvé pour cette course\"}");
                return;
            }

            Message message = optMessage.get();

            if (!isAdmin && (currentUser == null || message.getEmetteur().getIdParticipant() != currentUser.getIdParticipant())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Vous n'êtes pas autorisé à supprimer ce message\"}");
                return;
            }

            boolean deleted = messageService.deleteMessage(messageId);

            if (deleted) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\": \"Message supprimé avec succès\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Échec de la suppression du message\"}");
            }

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }
}
