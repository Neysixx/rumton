package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.service.EmailService;
import fr.esgi.color_run.util.DebugUtil;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailServiceImpl implements EmailService {
    private static final Properties properties = new Properties();
    private static final String SMTP_HOST;
    private static final String SMTP_PORT;
    private static final String USERNAME;
    private static final String PASSWORD;
    private static final String BASE_URL;
    
    static {
        try {
            // Load properties from application.properties file
            properties.load(EmailServiceImpl.class.getClassLoader().getResourceAsStream("application.properties"));
            SMTP_HOST = properties.getProperty("SMTP_HOST");
            SMTP_PORT = properties.getProperty("SMTP_PORT");
            USERNAME = properties.getProperty("SMTP_USERNAME");
            PASSWORD = properties.getProperty("SMTP_PASSWORD");
            BASE_URL = properties.getProperty("BASE_URL");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des propriétés", e);
        }
    }

    @Override
    public String envoyerEmailVerification(String email, String code) throws MessagingException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email ne peut pas être null ou vide");
        }

        String verificationUrl = BASE_URL + "/verify?email=" + email;

        // Corps du message en HTML avec un design moderne
        String htmlContent = getEmailTemplate(
            "Vérification de votre adresse email",
            "Bienvenue sur Rumton ! 🎨",
            "<p style='font-size: 16px; line-height: 1.6; color: #333; margin-bottom: 20px;'>"
            + "Merci de vous être inscrit à notre plateforme de Color Run ! Nous sommes ravis de vous compter parmi nous."
            + "</p>"
            + "<p style='font-size: 16px; line-height: 1.6; color: #333; margin-bottom: 30px;'>"
            + "Pour finaliser votre inscription, veuillez cliquer sur le bouton ci-dessous pour vérifier votre adresse email :"
            + "</p>"
            + "<div style='text-align: center; margin: 30px 0;'>"
            + "<a href='" + verificationUrl + "' style='display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);'>✓ Vérifier mon email</a>"
            + "</div>"
            + "<p style='font-size: 14px; color: #666; margin-top: 20px;'>"
            + "Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :<br>"
            + "<a href='" + verificationUrl + "' style='color: #667eea; word-break: break-all;'>" + verificationUrl + "</a>"
            + "</p>"
            + "<div style='background: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center;'>"
            + "<p style='margin: 0; font-size: 16px; color: #333;'><strong>Code de vérification :</strong></p>"
            + "<p style='margin: 10px 0 0; font-size: 24px; font-weight: bold; color: #667eea; letter-spacing: 2px;'>" + code + "</p>"
            + "</div>"
        );

        envoyerEmail(email, "Vérification de votre adresse email", htmlContent);

        return code;
    }

    /**
     ** Génère un code de vérification aléatoire
     * 
     * @return Le code de vérification
     */
    @Override
    public String genererCodeVerification() {
        int code = (int) (Math.random() * 900000) + 100000; // Génère un nombre aléatoire entre 100000 et 999999
        return String.valueOf(code);
    }

    @Override
    public void envoyerEmail(String destinataire, String sujet, String contenu) throws MessagingException {
        if (destinataire == null || destinataire.trim().isEmpty()) {
            throw new IllegalArgumentException("Le destinataire ne peut pas être null ou vide");
        }
        if (sujet == null || sujet.trim().isEmpty()) {
            throw new IllegalArgumentException("Le sujet ne peut pas être null ou vide");
        }
        if (contenu == null || contenu.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu ne peut pas être null ou vide");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        try {
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(sujet);
            message.setContent(contenu, "text/html; charset=utf-8");

            Transport.send(message);
        } catch (MessagingException e) {
            DebugUtil.log(this.getClass(), "Erreur lors de l'envoi d'email: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void envoyerDossardEmail(String email, int idParticipation) throws MessagingException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email ne peut pas être null ou vide");
        }

        String downloadLink = BASE_URL + "/dossard/" + idParticipation;

        // Corps du message en HTML avec un design moderne
        String htmlContent = getEmailTemplate(
            "Téléchargement de votre dossard",
            "Votre dossard est prêt ! 🏃‍♀️",
            "<p style='font-size: 16px; line-height: 1.6; color: #333; margin-bottom: 20px;'>"
            + "Félicitations ! Vous êtes maintenant inscrit(e) à une course Color Run. L'aventure colorée vous attend !"
            + "</p>"
            + "<p style='font-size: 16px; line-height: 1.6; color: #333; margin-bottom: 30px;'>"
            + "Votre dossard personnalisé est prêt. Cliquez sur le bouton ci-dessous pour le télécharger :"
            + "</p>"
            + "<div style='text-align: center; margin: 30px 0;'>"
            + "<a href='" + downloadLink + "' style='display: inline-block; background: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);'>📥 Télécharger mon dossard</a>"
            + "</div>"
            + "<p style='font-size: 14px; color: #666; margin-top: 20px;'>"
            + "Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :<br>"
            + "<a href='" + downloadLink + "' style='color: #FF6B6B; word-break: break-all;'>" + downloadLink + "</a>"
            + "</p>"
            + "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; padding: 20px; margin: 20px 0; color: white; text-align: center;'>"
            + "<p style='margin: 0; font-size: 16px; font-weight: bold;'>💡 Conseils pour le jour J :</p>"
            + "<p style='margin: 10px 0 0; font-size: 14px; line-height: 1.5;'>"
            + "• Portez du blanc pour un effet maximal<br>"
            + "• Apportez votre dossard imprimé<br>"
            + "• Préparez-vous à vivre une expérience colorée unique !"
            + "</p>"
            + "</div>"
        );

        envoyerEmail(email, "Téléchargement de votre dossard", htmlContent);
    }

    /**
     * Méthode utilitaire pour créer un template d'email avec un design moderne
     */
    private String getEmailTemplate(String title, String subtitle, String content) {
        return "<!DOCTYPE html>"
            + "<html lang='fr'>"
            + "<head>"
            + "<meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<title>" + title + "</title>"
            + "</head>"
            + "<body style='margin: 0; padding: 0; background-color: #f5f5f5; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif;'>"
            + "<div style='max-width: 600px; margin: 0 auto; background-color: white; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>"
            
            // Header avec gradient coloré
            + "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 20px; text-align: center;'>"
            + "<h1 style='color: white; margin: 0; font-size: 28px; font-weight: bold;'>" + title + "</h1>"
            + "<p style='color: rgba(255,255,255,0.9); margin: 10px 0 0; font-size: 18px;'>" + subtitle + "</p>"
            + "</div>"
            
            // Logo/Brand section
            + "<div style='text-align: center; padding: 30px 20px 20px; background: white;'>"
            + "<div style='display: inline-block; background: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%); color: white; padding: 10px 20px; border-radius: 20px; font-weight: bold; font-size: 20px;'>"
            + "🎨 RUMTON"
            + "</div>"
            + "</div>"
            
            // Content
            + "<div style='padding: 0 30px 40px; background: white;'>"
            + content
            + "</div>"
            
            // Footer
            + "<div style='background: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;'>"
            + "<p style='margin: 0; color: #666; font-size: 14px;'>"
            + "Cet email a été envoyé par <strong>Rumton</strong><br>"
            + "L'expérience Color Run qui transforme chaque course en fête colorée !"
            + "</p>"
            + "<div style='margin-top: 20px;'>"
            + "<a href='#' style='color: #667eea; text-decoration: none; margin: 0 10px;'>📧 Contact</a>"
            + "<a href='#' style='color: #667eea; text-decoration: none; margin: 0 10px;'>📘 Facebook</a>"
            + "<a href='#' style='color: #667eea; text-decoration: none; margin: 0 10px;'>📸 Instagram</a>"
            + "</div>"
            + "<p style='margin: 20px 0 0; color: #999; font-size: 12px;'>"
            + "© 2025 Rumton. Tous droits réservés."
            + "</p>"
            + "</div>"
            
            + "</div>"
            + "</body>"
            + "</html>";
    }

    /**
     * Envoie un email de réinitialisation de mot de passe avec un design moderne
     */
    public void envoyerEmailReinitialisationMotDePasse(String email, String resetUrl) throws MessagingException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email ne peut pas être null ou vide");
        }

        String htmlContent = getEmailTemplate(
            "Réinitialisation de votre mot de passe",
            "Récupération de votre compte 🔐",
            "<p style='font-size: 16px; line-height: 1.6; color: #333; margin-bottom: 20px;'>"
            + "Vous avez demandé une réinitialisation de votre mot de passe. Pas de souci, cela arrive aux meilleurs !"
            + "</p>"
            + "<p style='font-size: 16px; line-height: 1.6; color: #333; margin-bottom: 30px;'>"
            + "Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe sécurisé :"
            + "</p>"
            + "<div style='text-align: center; margin: 30px 0;'>"
            + "<a href='" + resetUrl + "' style='display: inline-block; background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; box-shadow: 0 4px 15px rgba(40, 167, 69, 0.4);'>🔑 Réinitialiser mon mot de passe</a>"
            + "</div>"
            + "<p style='font-size: 14px; color: #666; margin-top: 20px;'>"
            + "Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :<br>"
            + "<a href='" + resetUrl + "' style='color: #28a745; word-break: break-all;'>" + resetUrl + "</a>"
            + "</p>"
            + "<div style='background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 8px; padding: 15px; margin: 20px 0;'>"
            + "<p style='margin: 0; font-size: 14px; color: #856404;'>"
            + "<strong>⚠️ Important :</strong> Ce lien est valable pendant 1 heure seulement. Si vous n'avez pas demandé cette réinitialisation, ignorez cet email."
            + "</p>"
            + "</div>"
        );

        envoyerEmail(email, "Réinitialisation de votre mot de passe", htmlContent);
    }
}