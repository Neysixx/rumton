package fr.esgi.color_run.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Message;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.MessageService;
import fr.esgi.color_run.service.ParticipationService;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import fr.esgi.color_run.service.impl.MessageServiceImpl;
import fr.esgi.color_run.service.impl.ParticipationServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
public class MessageServlet extends BaseWebServlet {

    private MessageService messageService;
    private CourseService courseService;
    private ParticipationService participationService;

    @Override
    public void init() {
        super.init();
        messageService = new MessageServiceImpl();
        courseService = new CourseServiceImpl();
        participationService = new ParticipationServiceImpl();
    }

    /**
     * Extraire l'ID du message à partir du chemin de l'URL /messages/{messageId}
     */
    private int extractMessageId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            throw new IllegalArgumentException("Aucun ID de message spécifié");
        }
        
        String[] pathParts = pathInfo.split("/");
        String idStr = pathParts[pathParts.length > 1 ? 1 : 0];
        
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
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        Context context = new Context();
        String pathInfo = request.getPathInfo();

        try {
            // /messages/X : récupérer un message spécifique
            if (pathInfo != null && !pathInfo.equals("/")) {
                int messageId = extractMessageId(request);
                
                Optional<Message> optMessage = messageService.getMessageById(messageId);
                if (optMessage.isEmpty()) {
                    renderError(request, response, "Message non trouvé");
                    return;
                }

                Message message = optMessage.get();
                context.setVariable("message", message);

                List<Message> replies = messageService.getRepliesByParent(messageId);
                context.setVariable("replies", replies);
                context.setVariable("isAdmin", request.getAttribute("is_admin"));
                context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));

                renderTemplate(request, response, "message", context);
            } 
            // /messages?courseId=X : liste des messages d'une course
            else {
                String courseIdStr = request.getParameter("courseId");
                
                if (courseIdStr == null || courseIdStr.isEmpty()) {
                    renderError(request, response, "Le paramètre courseId est requis");
                    return;
                }
                
                int courseId;
                try {
                    courseId = Integer.parseInt(courseIdStr);
                } catch (NumberFormatException e) {
                    renderError(request, response, "ID de course invalide: " + courseIdStr);
                    return;
                }

                Optional<Course> optCourse = courseService.getCourseById(courseId);
                if (optCourse.isEmpty()) {
                    renderError(request, response, "Course non trouvée");
                    return;
                }

                List<Message> messages = messageService.getMessagesByCourse(courseId);
                context.setVariable("messages", messages);
                context.setVariable("course", optCourse.get());
                context.setVariable("isAdmin", request.getAttribute("is_admin"));
                context.setVariable("isOrganisateur", request.getAttribute("is_organisateur"));

                renderTemplate(request, response, "messages", context);
            }
        } catch (IllegalArgumentException e) {
            renderError(request, response, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    /**
     * Traite les requêtes POST pour créer un nouveau message
     * POST /messages avec courseId, contenu et éventuellement messageParentId
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            BufferedReader reader = request.getReader();
            String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            Map<String, String> params = parseUrlEncodedBody(body);
            Participant participant = getAuthenticatedParticipant(request);
            if (participant == null) {
                renderError(request, response, "Impossible de récupérer les informations du participant");
                return;
            }

            String contenu = params.get("contenu");
            if (contenu == null || contenu.trim().isEmpty()) {
                renderError(request, response, "Le contenu du message est obligatoire");
                return;
            }

            String courseIdStr = params.get("courseId");
            if (courseIdStr == null || courseIdStr.trim().isEmpty()) {
                renderError(request, response, "L'ID de course est obligatoire");
                return;
            }

            int courseId;
            try {
                courseId = Integer.parseInt(courseIdStr);
            } catch (NumberFormatException e) {
                renderError(request, response, "ID de course invalide: " + courseIdStr);
                return;
            }

            Optional<Course> optCourse = courseService.getCourseById(courseId);
            if (optCourse.isEmpty()) {
                renderError(request, response, "Course non trouvée avec l'ID " + courseId);
                return;
            }

            if(!participationService.isParticipantRegistered(participant.getIdParticipant(), courseId)){
                renderError(request, response, "Vous devez être inscrit à cette course pour pouvoir parler");
                return;
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
                        renderError(request, response, "Message parent invalide ou n'appartient pas à la même course");
                        return;
                    }
                } catch (NumberFormatException e) {
                    renderError(request, response, "ID de message parent invalide: " + parentIdStr);
                    return;
                }
            }

            messageService.createMessage(message);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            // Créer une réponse simple
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", message.getContenu());
            responseData.put("sender", message.getEmetteur());
            responseData.put("createdAt", message.getDatePublication());

            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(responseData);
            response.getWriter().write(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            int messageId = extractMessageId(request);
            Optional<Message> optMessage = messageService.getMessageById(messageId);
            if (optMessage.isEmpty()) {
                renderError(request, response, "Message non trouvé");
                return;
            }

            Message message = optMessage.get();
            Participant currentUser = getAuthenticatedParticipant(request);

            // Vérification que l'utilisateur est l'auteur du message
            if (message.getEmetteur().getIdParticipant() != currentUser.getIdParticipant()) {
                renderError(request, response, "Vous n'êtes pas autorisé à modifier ce message");
                return;
            }

            String contenu = request.getParameter("contenu");
            if (contenu == null || contenu.trim().isEmpty()) {
                renderError(request, response, "Le contenu du message est obligatoire");
                return;
            }

            message.setContenu(contenu);
            messageService.updateMessage(message);

            // Redirection vers la liste des messages de la course
            response.sendRedirect(request.getContextPath() + "/messages?courseId=" + message.getCourse().getIdCourse());

        } catch (IllegalArgumentException e) {
            renderError(request, response, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            int messageId = extractMessageId(request);
            Optional<Message> optMessage = messageService.getMessageById(messageId);
            if (optMessage.isEmpty()) {
                renderError(request, response, "Message non trouvé");
                return;
            }

            Message message = optMessage.get();
            Participant currentUser = getAuthenticatedParticipant(request);

            // Vérification que l'utilisateur est l'auteur du message
            if (message.getEmetteur().getIdParticipant() != currentUser.getIdParticipant()) {
                renderError(request, response, "Vous n'êtes pas autorisé à supprimer ce message");
                return;
            }

            int courseId = message.getCourse().getIdCourse();
            messageService.deleteMessage(messageId);

            // Redirection vers la liste des messages de la course
            response.sendRedirect(request.getContextPath() + "/messages?courseId=" + courseId);

        } catch (IllegalArgumentException e) {
            renderError(request, response, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur est survenue: " + e.getMessage());
        }
    }
}
