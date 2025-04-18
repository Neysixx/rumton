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
 * Utilise des routes plus simples:
 * - GET /messages?courseId=X pour lister les messages d'une course
 * - GET /messages/X pour récupérer un message spécifique
 * - POST /messages pour créer un message
 * - PUT /messages/X pour modifier un message
 * - DELETE /messages/X pour supprimer un message
 */
@WebServlet(name = "messageServlet", value = {"/messages", "/messages/*"})
public class MessageServlet extends HttpServlet {

    private AuthService authService;
    private MessageService messageService;
    private CourseService courseService;

    @Override
    public void init() {
        System.out.println("DEBUG - Initialisation de MessageServlet");
        authService = new AuthServiceImpl();
        messageService = new MessageServiceImpl();
        courseService = new CourseServiceImpl();
    }

    /**
     * Extraire l'ID du message à partir du chemin de l'URL /messages/{messageId}
     */
    private int extractMessageId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        System.out.println("DEBUG - PathInfo: " + pathInfo);
        
        if (pathInfo == null || pathInfo.equals("/")) {
            throw new IllegalArgumentException("Aucun ID de message spécifié");
        }
        
        String[] pathParts = pathInfo.split("/");
        String idStr = pathParts[pathParts.length > 1 ? 1 : 0];
        System.out.println("DEBUG - ID extrait de l'URL: " + idStr);
        
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID de message invalide: " + idStr);
        }
    }

    /**
     * Traite les requêtes GET pour afficher les messages
     * - GET /messages?courseId=X : liste des messages d'une course
     * - GET /messages/X : détail d'un message spécifique
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("DEBUG - Début doGet: " + request.getRequestURI());
        
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        Context context = new Context();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        System.out.println("DEBUG - PathInfo: " + pathInfo);
        System.out.println("DEBUG - QueryString: " + request.getQueryString());

        try {
            // /messages/X : récupérer un message spécifique
            if (pathInfo != null && !pathInfo.equals("/")) {
                int messageId = extractMessageId(request);
                
                Optional<Message> optMessage = messageService.getMessageById(messageId);
                if (optMessage.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Message non trouvé\"}");
                    return;
                }

                Message message = optMessage.get();
                context.setVariable("message", message);

                List<Message> replies = messageService.getRepliesByParent(messageId);
                context.setVariable("replies", replies);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\":" + message.toString() + ", \"replies\":" + replies.toString() + "}");
                templateEngine.process("message", context, response.getWriter());
            } 
            // /messages?courseId=X : liste des messages d'une course
            else {
                String courseIdStr = request.getParameter("courseId");
                System.out.println("DEBUG - courseId parameter: " + courseIdStr);
                
                if (courseIdStr == null || courseIdStr.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Le paramètre courseId est requis\"}");
                    return;
                }
                
                int courseId;
                try {
                    courseId = Integer.parseInt(courseIdStr);
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"ID de course invalide: " + courseIdStr + "\"}");
                    return;
                }

                Optional<Course> optCourse = courseService.getCourseById(courseId);
                if (optCourse.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Course non trouvée\"}");
                    return;
                }

                List<Message> messages = messageService.getMessagesByCourse(courseId);
                System.out.println("DEBUG - Nombre de messages trouvés: " + messages.size());

                context.setVariable("messages", messages);
                context.setVariable("course", optCourse.get());

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(messages.toString());
                templateEngine.process("messages", context, response.getWriter());
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
        System.out.println("DEBUG - Fin doGet");
    }

    /**
     * Traite les requêtes POST pour créer un nouveau message
     * POST /messages avec courseId, contenu et éventuellement messageParentId
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("DEBUG - Début doPost");
        
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

            String courseIdStr = request.getParameter("courseId");
            if (courseIdStr == null || courseIdStr.trim().isEmpty()) {
                throw new IllegalArgumentException("L'ID de course est obligatoire");
            }

            int courseId;
            try {
                courseId = Integer.parseInt(courseIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ID de course invalide: " + courseIdStr);
            }

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
                try {
                    int parentId = Integer.parseInt(parentIdStr);
                    Optional<Message> optParent = messageService.getMessageById(parentId);
                    if (optParent.isPresent() && optParent.get().getCourse().getIdCourse() == courseId) {
                        message.setMessageParent(optParent.get());
                    } else {
                        throw new IllegalArgumentException("Message parent invalide ou n'appartient pas à la même course");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("ID de message parent invalide: " + parentIdStr);
                }
            }

            messageService.createMessage(message);
            
            System.out.println("DEBUG - Message créé avec ID: " + message.getIdMessage());

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
        System.out.println("DEBUG - Fin doPost");
    }

    /**
     * Traite les requêtes PUT pour modifier un message
     * PUT /messages/X
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("DEBUG - Début doPut");
        
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

            // Vérifier si l'URL contient un ID de message
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"L'ID du message est requis pour la modification\"}");
                return;
            }

            int messageId = extractMessageId(request);
            System.out.println("DEBUG - ID du message à modifier: " + messageId);

            Optional<Message> optMessage = messageService.getMessageById(messageId);
            if (optMessage.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Message non trouvé\"}");
                return;
            }

            Message message = optMessage.get();

            // Vérifier si l'utilisateur est autorisé à modifier le message
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
        System.out.println("DEBUG - Fin doPut");
    }

    /**
     * Traite les requêtes DELETE pour supprimer un message
     * DELETE /messages/X
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("DEBUG - Début doDelete");
        
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            Participant currentUser = authService.getParticipantFromToken(token);
            boolean isAdmin = authService.isAdmin(token);

            // Vérifier si l'URL contient un ID de message
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"L'ID du message est requis pour la suppression\"}");
                return;
            }

            int messageId = extractMessageId(request);
            System.out.println("DEBUG - ID du message à supprimer: " + messageId);

            Optional<Message> optMessage = messageService.getMessageById(messageId);
            if (optMessage.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Message non trouvé\"}");
                return;
            }

            Message message = optMessage.get();

            // Vérifier si l'utilisateur est autorisé à supprimer le message
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
        System.out.println("DEBUG - Fin doDelete");
    }
}
