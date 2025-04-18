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
@WebServlet(name = "messageServlet", value = {"/messages/*", "/course/*/messages", "/course/*/messages/*"})
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
     * Traite les requêtes GET pour afficher les messages
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        // On récupère le moteur de template dans le contexte des servlets
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
        Context context = new Context();

        // Configuration de la réponse
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Analyse du chemin pour déterminer le type de requête
        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();

        try {
            // URL pattern: /course/{courseId}/messages
            if (servletPath.matches("/course/\\d+/messages")) {
                // Extraction de l'ID de la course
                int courseId = extractCourseIdFromPath(servletPath);
                
                // Vérification que la course existe
                Optional<Course> optCourse = courseService.getCourseById(courseId);
                if (optCourse.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Course non trouvée\"}");
                    return;
                }
                
                // Récupération des messages pour cette course
                List<Message> messages = messageService.getMessagesByCourse(courseId);
                
                // Ajout à la vue
                context.setVariable("messages", messages);
                context.setVariable("course", optCourse.get());
                
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(messages.toString());
                templateEngine.process("messages", context, response.getWriter());
            }
            // URL pattern: /course/{courseId}/messages/{messageId}
            else if (servletPath.matches("/course/\\d+/messages/\\d+")) {
                // Extraction des IDs
                int courseId = extractCourseIdFromPath(servletPath);
                int messageId = extractMessageIdFromPath(pathInfo);
                
                // Récupération du message
                Optional<Message> optMessage = messageService.getMessageById(messageId);
                if (optMessage.isEmpty() || optMessage.get().getCourse().getIdCourse() != courseId) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Message non trouvé\"}");
                    return;
                }
                
                // Ajout à la vue
                context.setVariable("message", optMessage.get());
                
                // Si c'est un message parent, récupérer ses réponses
                List<Message> replies = messageService.getRepliesByParent(messageId);
                context.setVariable("replies", replies);
                
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(optMessage.get().toString());
                templateEngine.process("message", context, response.getWriter());
            }
            // URL pattern: /messages/{messageId}
            else if (servletPath.equals("/messages") && pathInfo != null && pathInfo.matches("/\\d+")) {
                int messageId = Integer.parseInt(pathInfo.substring(1));
                
                // Récupération du message
                Optional<Message> optMessage = messageService.getMessageById(messageId);
                if (optMessage.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Message non trouvé\"}");
                    return;
                }
                
                // Ajout à la vue
                context.setVariable("message", optMessage.get());
                
                // Si c'est un message parent, récupérer ses réponses
                List<Message> replies = messageService.getRepliesByParent(messageId);
                context.setVariable("replies", replies);
                
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(optMessage.get().toString());
                templateEngine.process("message", context, response.getWriter());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Requête invalide\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID invalide\"}");
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
            String contenu = request.getParameter("contenu");
            if (contenu == null || contenu.trim().isEmpty()) {
                throw new IllegalArgumentException("Le contenu du message est obligatoire");
            }
            
            String servletPath = request.getServletPath();
            
            // URL pattern: /course/{courseId}/messages
            if (servletPath.matches("/course/\\d+/messages")) {
                int courseId = extractCourseIdFromPath(servletPath);
                
                // Vérification que la course existe
                Optional<Course> optCourse = courseService.getCourseById(courseId);
                if (optCourse.isEmpty()) {
                    throw new IllegalArgumentException("Course non trouvée avec l'ID " + courseId);
                }
                
                Course course = optCourse.get();
                
                // Création du message
                Message message = Message.builder()
                        .emetteur(participant)
                        .course(course)
                        .contenu(contenu)
                        .datePublication(new Date())
                        .build();
                
                // Vérifier si c'est une réponse à un autre message
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
                
                // Enregistrement du message
                messageService.createMessage(message);
                
                // Réponse
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write("{\"message\": \"Message publié avec succès\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"URL invalide pour la création d'un message\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID invalide\"}");
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
        // Vérification de l'authentification
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            // Récupération de l'utilisateur courant
            Participant currentUser = authService.getParticipantFromToken(token);
            boolean isAdmin = authService.isAdmin(token);
            
            if (currentUser == null && !isAdmin) {
                throw new ServletException("Impossible de récupérer les informations de l'utilisateur");
            }
            
            // Analyse du chemin pour déterminer l'ID du message
            String pathInfo = request.getPathInfo();
            String servletPath = request.getServletPath();
            
            int messageId;
            
            // URL pattern: /messages/{messageId} ou /course/{courseId}/messages/{messageId}
            if (servletPath.equals("/messages") && pathInfo != null && pathInfo.matches("/\\d+")) {
                messageId = Integer.parseInt(pathInfo.substring(1));
            } else if (servletPath.matches("/course/\\d+/messages") && pathInfo != null && pathInfo.matches("/\\d+")) {
                messageId = Integer.parseInt(pathInfo.substring(1));
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"URL invalide pour la modification d'un message\"}");
                return;
            }
            
            // Récupération du message
            Optional<Message> optMessage = messageService.getMessageById(messageId);
            if (optMessage.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Message non trouvé\"}");
                return;
            }
            
            Message message = optMessage.get();
            
            // Vérification des permissions
            if (!isAdmin && (currentUser == null || message.getEmetteur().getIdParticipant() != currentUser.getIdParticipant())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Vous n'êtes pas autorisé à modifier ce message\"}");
                return;
            }
            
            // Récupération du nouveau contenu
            String contenu = request.getParameter("contenu");
            if (contenu == null || contenu.trim().isEmpty()) {
                throw new IllegalArgumentException("Le contenu du message est obligatoire");
            }
            
            // Mise à jour du message
            message.setContenu(contenu);
            messageService.updateMessage(message);
            
            // Réponse
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"message\": \"Message modifié avec succès\"}");
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID invalide\"}");
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
        // Vérification de l'authentification
        String token = (String) request.getAttribute("jwt_token");
        if (token == null || !authService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Utilisateur non authentifié\"}");
            return;
        }

        try {
            // Récupération de l'utilisateur courant
            Participant currentUser = authService.getParticipantFromToken(token);
            boolean isAdmin = authService.isAdmin(token);
            
            // Analyse du chemin pour déterminer l'ID du message
            String pathInfo = request.getPathInfo();
            String servletPath = request.getServletPath();
            
            int messageId;
            
            // URL pattern: /messages/{messageId} ou /course/{courseId}/messages/{messageId}
            if (servletPath.equals("/messages") && pathInfo != null && pathInfo.matches("/\\d+")) {
                messageId = Integer.parseInt(pathInfo.substring(1));
            } else if (servletPath.matches("/course/\\d+/messages") && pathInfo != null && pathInfo.matches("/\\d+")) {
                messageId = Integer.parseInt(pathInfo.substring(1));
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"URL invalide pour la suppression d'un message\"}");
                return;
            }
            
            // Récupération du message
            Optional<Message> optMessage = messageService.getMessageById(messageId);
            if (optMessage.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Message non trouvé\"}");
                return;
            }
            
            Message message = optMessage.get();
            
            // Vérification des permissions (l'auteur du message ou un admin)
            if (!isAdmin && (currentUser == null || message.getEmetteur().getIdParticipant() != currentUser.getIdParticipant())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Vous n'êtes pas autorisé à supprimer ce message\"}");
                return;
            }
            
            // Suppression du message
            boolean deleted = messageService.deleteMessage(messageId);
            
            if (deleted) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\": \"Message supprimé avec succès\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Échec de la suppression du message\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Format d'ID invalide\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Une erreur est survenue: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Extrait l'ID de la course à partir du chemin de la requête
     */
    private int extractCourseIdFromPath(String path) {
        // Format attendu: /course/{courseId}/messages
        String[] parts = path.split("/");
        return Integer.parseInt(parts[2]);
    }

    /**
     * Extrait l'ID du message à partir du chemin de la requête
     */
    private int extractMessageIdFromPath(String pathInfo) {
        // Format attendu: /{messageId}
        return Integer.parseInt(pathInfo.substring(1));
    }
}
