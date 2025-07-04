package fr.esgi.color_run.servlet;

import fr.esgi.color_run.service.EmailService;
import fr.esgi.color_run.service.impl.EmailServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Properties;

/**
 * Servlet qui gère la documentaion de l'app "les footers etc"
 */
@WebServlet(name = "documentationServlet", value = {"/faq", "/about-us", "/contact", "/legal-mentions"})
public class DocumentationServlet extends BaseWebServlet{

    private EmailService emailService;
    private static final Properties properties = new Properties();
    private static String SUPPORT_EMAIL;

    @Override
    public void init() {
        super.init();
        emailService = new EmailServiceImpl();
        try {
            properties.load(EmailServiceImpl.class.getClassLoader().getResourceAsStream("application.properties"));
            SUPPORT_EMAIL = properties.getProperty("SMTP_USERNAME");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Création du contexte Thymeleaf
        Context context = new Context();

        switch (request.getServletPath()) {
            case "/faq":
                renderTemplate(request, response, "doc/faq", context);
                break;
            case "/about-us":
                renderTemplate(request, response, "doc/aboutUs", context);
                break;
            case "/contact":
                renderTemplate(request, response, "doc/contact", context);
                break;
            case "/legal-mentions":
                renderTemplate(request, response, "doc/legalMentions", context);
                break;


        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Traiter uniquement le formulaire de contact
        if (!"/contact".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        try {
            // Récupération des données du formulaire
            String name = request.getParameter("name");
            String email = request.getParameter("email");
            String message = request.getParameter("message");

            // Validation
            if (name == null || name.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                message == null || message.trim().isEmpty()) {
                
                Context context = new Context();
                context.setVariable("error", "Tous les champs sont obligatoires.");
                renderTemplate(request, response, "doc/contact", context);
                return;
            }

            // Email pour le support
            String supportEmail = SUPPORT_EMAIL;
            String supportSubject = "Nouveau message de contact - " + name;
            String supportContent = createContactSupportEmail(name, email, message);
            
            // Email de confirmation pour l'expéditeur
            String confirmationSubject = "Confirmation de réception - Rumton";
            String confirmationContent = createContactConfirmationEmail(name, message);

            // Envoi des emails
            emailService.envoyerEmail(supportEmail, supportSubject, supportContent);
            emailService.envoyerEmail(email, confirmationSubject, confirmationContent);

            // Message de succès
            Context context = new Context();
            context.setVariable("success", "Votre message a été envoyé avec succès ! Nous vous répondrons dans les plus brefs délais.");
            renderTemplate(request, response, "doc/contact", context);

        } catch (MessagingException e) {
            e.printStackTrace();
            Context context = new Context();
            context.setVariable("error", "Une erreur est survenue lors de l'envoi du message. Veuillez réessayer.");
            renderTemplate(request, response, "doc/contact", context);
        } catch (Exception e) {
            e.printStackTrace();
            renderError(request, response, "Une erreur inattendue est survenue.");
        }
    }

    /**
     * Crée le contenu de l'email envoyé au support
     */
    private String createContactSupportEmail(String name, String email, String message) {
        return "<!DOCTYPE html>"
            + "<html lang='fr'>"
            + "<head><meta charset='UTF-8'><title>Nouveau message de contact</title></head>"
            + "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
            + "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>"
            + "<h2 style='color: #667eea;'>📧 Nouveau message de contact</h2>"
            + "<div style='background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;'>"
            + "<p><strong>Nom :</strong> " + name + "</p>"
            + "<p><strong>Email :</strong> " + email + "</p>"
            + "<p><strong>Date :</strong> " + new java.util.Date() + "</p>"
            + "</div>"
            + "<div style='background: white; padding: 20px; border: 1px solid #ddd; border-radius: 8px;'>"
            + "<h3>Message :</h3>"
            + "<p style='white-space: pre-wrap;'>" + message + "</p>"
            + "</div>"
            + "<div style='margin-top: 20px; padding: 15px; background: #e3f2fd; border-radius: 8px;'>"
            + "<p style='margin: 0; font-size: 14px; color: #1976d2;'>"
            + "<strong>Action requise :</strong> Répondre à " + email + " dans les plus brefs délais."
            + "</p>"
            + "</div>"
            + "</div></body></html>";
    }

    /**
     * Crée le contenu de l'email de confirmation pour l'expéditeur
     */
    private String createContactConfirmationEmail(String name, String message) {
        return "<!DOCTYPE html>"
            + "<html lang='fr'>"
            + "<head><meta charset='UTF-8'><title>Confirmation de réception</title></head>"
            + "<body style='margin: 0; padding: 0; background-color: #f5f5f5; font-family: Arial, sans-serif;'>"
            + "<div style='max-width: 600px; margin: 0 auto; background-color: white; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>"
            
            // Header
            + "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 20px; text-align: center;'>"
            + "<h1 style='color: white; margin: 0; font-size: 28px;'>Message bien reçu ! ✅</h1>"
            + "<p style='color: rgba(255,255,255,0.9); margin: 10px 0 0; font-size: 18px;'>Merci pour votre contact</p>"
            + "</div>"
            
            // Logo
            + "<div style='text-align: center; padding: 30px 20px 20px;'>"
            + "<div style='display: inline-block; background: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%); color: white; padding: 10px 20px; border-radius: 20px; font-weight: bold; font-size: 20px;'>"
            + "🎨 RUMTON"
            + "</div>"
            + "</div>"
            
            // Content
            + "<div style='padding: 0 30px 40px;'>"
            + "<p style='font-size: 16px; line-height: 1.6; color: #333;'>Bonjour " + name + ",</p>"
            + "<p style='font-size: 16px; line-height: 1.6; color: #333;'>"
            + "Nous avons bien reçu votre message et nous vous remercions de nous avoir contactés ! 🙏"
            + "</p>"
            + "<p style='font-size: 16px; line-height: 1.6; color: #333;'>"
            + "Notre équipe examine votre demande et vous répondra dans les plus brefs délais, généralement sous 24-48h."
            + "</p>"
            
            // Recap du message
            + "<div style='background: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;'>"
            + "<h3 style='margin: 0 0 15px; color: #667eea;'>📝 Récapitulatif de votre message :</h3>"
            + "<p style='margin: 0; white-space: pre-wrap; font-style: italic; color: #666;'>" + message + "</p>"
            + "</div>"
            
            + "<p style='font-size: 16px; line-height: 1.6; color: #333;'>"
            + "En attendant notre réponse, n'hésitez pas à découvrir nos prochaines courses colorées sur notre site !"
            + "</p>"
            
            + "<div style='text-align: center; margin: 30px 0;'>"
            + "<a href='#' style='display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 25px; font-weight: bold;'>🏃‍♀️ Voir les courses</a>"
            + "</div>"
            + "</div>"
            
            // Footer
            + "<div style='background: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;'>"
            + "<p style='margin: 0; color: #666; font-size: 14px;'>Merci de faire confiance à <strong>Rumton</strong> !</p>"
            + "<p style='margin: 10px 0 0; color: #999; font-size: 12px;'>© 2025 Rumton. Tous droits réservés.</p>"
            + "</div>"
            
            + "</div></body></html>";
    }
}
