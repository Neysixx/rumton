package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Participation;
import fr.esgi.color_run.service.*;
import fr.esgi.color_run.service.impl.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;

/**
 * Servlet pour gérer les paiements Stripe
 */
@WebServlet(name = "paymentServlet", value = {"/payment/*", "/payment-success", "/payment-cancel"})
public class PaymentServlet extends BaseWebServlet {

    private CourseService courseService;
    private ParticipationService participationService;
    private StripeService stripeService;

    @Override
    public void init() {
        super.init();
        courseService = new CourseServiceImpl();
        participationService = new ParticipationServiceImpl();
        stripeService = new StripeServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();

        if ("/payment-success".equals(servletPath)) {
            handlePaymentSuccess(request, response);
        } else if ("/payment-cancel".equals(servletPath)) {
            handlePaymentCancel(request, response);
        } else if ("/payment".equals(servletPath) && pathInfo != null && pathInfo.length() > 1) {
            // Initier un paiement pour une course
            handlePaymentInitiation(request, response);
        }
    }

    /**
     * Initie le processus de paiement pour une course
     */
    private void handlePaymentInitiation(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Vérification de l'authentification
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            String pathInfo = request.getPathInfo();
            int courseId = Integer.parseInt(pathInfo.substring(1));

            // Récupérer la course
            Course course = courseService.getCourseById(courseId).orElseThrow(() -> new IllegalArgumentException("Course non trouvée"));

            // Vérifier que la course est payante
            if (course.getPrixParticipation() <= 0) {
                renderError(request, response, "Cette course est gratuite, aucun paiement requis");
                return;
            }

            // Vérifier que l'utilisateur n'est pas déjà inscrit
            Participant participant = getAuthenticatedParticipant(request);
            boolean isAlreadyRegistered = participationService.isParticipantRegistered(participant.getIdParticipant(), courseId);

            if (isAlreadyRegistered) {
                renderError(request, response, "Vous êtes déjà inscrit à cette course");
                return;
            }

            // Vérifier qu'il y a encore de la place
            int currentParticipants = participationService.getParticipationsByCourse(courseId).size();
            if (currentParticipants >= course.getMaxParticipants()) {
                renderError(request, response, "Cette course est complète");
                return;
            }

            // Vérifier que la course a un produit Stripe
            if (course.getStripeProductId() == null) {
                renderError(request, response, "Problème de configuration du paiement pour cette course");
                return;
            }

            // Créer la session de paiement Stripe
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
            String successUrl = baseUrl + "/payment-success?course_id=" + courseId;
            String cancelUrl = baseUrl + "/courses/" + courseId;

            String checkoutUrl = stripeService.createCheckoutSession(
                    course, 
                    participant.getEmail(), 
                    successUrl, 
                    cancelUrl
            );

            if (checkoutUrl != null) {
                // Rediriger vers Stripe Checkout
                response.sendRedirect(checkoutUrl);
            } else {
                renderError(request, response, "Erreur lors de la création de la session de paiement");
            }

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de course invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Erreur lors du processus de paiement : " + e.getMessage());
        }
    }

    /**
     * Gère le retour de succès après paiement
     */
    private void handlePaymentSuccess(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        if (!isAuthenticated(request, response)) {
            return;
        }

        try {
            String sessionId = request.getParameter("session_id");
            String courseIdParam = request.getParameter("course_id");

            if (sessionId == null || courseIdParam == null) {
                renderError(request, response, "Paramètres de paiement manquants");
                return;
            }

            int courseId = Integer.parseInt(courseIdParam);

            // Vérifier que le paiement a été complété
            boolean paymentCompleted = stripeService.isPaymentCompleted(sessionId);

            if (paymentCompleted) {
                // Inscrire le participant à la course
                Participant participant = getAuthenticatedParticipant(request);
                
                // Double vérification que l'utilisateur n'est pas déjà inscrit
                boolean isAlreadyRegistered = participationService.isParticipantRegistered(
                        participant.getIdParticipant(), courseId);

                if (!isAlreadyRegistered) {
                    // Créer la participation
                    int nextBibNumber = participationService.getLastBibNumberForCourse(courseId) + 1;
                    Participation participation = Participation.builder()
                            .participant(participant)
                            .course(courseService.getCourseById(courseId).orElse(null))
                            .numeroDossard(nextBibNumber)
                            .build();
                    participationService.createParticipation(participation);
                    
                    Context context = new Context();
                    context.setVariable("success", true);
                    context.setVariable("message", "Paiement réussi ! Vous êtes maintenant inscrit à la course.");
                    context.setVariable("courseId", courseId);
                    
                    renderTemplate(request, response, "payment/success", context);
                } else {
                    // Utilisateur déjà inscrit mais paiement validé
                    Context context = new Context();
                    context.setVariable("success", true);
                    context.setVariable("message", "Paiement déjà traité. Vous êtes inscrit à la course.");
                    context.setVariable("courseId", courseId);
                    
                    renderTemplate(request, response, "payment/success", context);
                }
            } else {
                renderError(request, response, "Le paiement n'a pas été confirmé");
            }

        } catch (NumberFormatException e) {
            renderError(request, response, "ID de course invalide");
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Erreur lors de la confirmation du paiement : " + e.getMessage());
        }
    }

    /**
     * Gère l'annulation du paiement
     */
    private void handlePaymentCancel(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Context context = new Context();
        context.setVariable("message", "Paiement annulé. Vous pouvez réessayer à tout moment.");
        
        String courseIdParam = request.getParameter("course_id");
        if (courseIdParam != null) {
            try {
                context.setVariable("courseId", Integer.parseInt(courseIdParam));
            } catch (NumberFormatException e) {
                // Ignorer si l'ID n'est pas valide
            }
        }
        
        renderTemplate(request, response, "payment/cancel", context);
    }
} 