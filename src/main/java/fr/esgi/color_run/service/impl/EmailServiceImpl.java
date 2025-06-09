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

        // Corps du message en HTML
        String htmlContent = "<h1>Vérification de votre adresse email</h1>"
                + "<p>Merci de vous être inscrit. Veuillez cliquer sur le lien ci-dessous pour vérifier votre adresse email :</p>"
                + "<p><a href='" + verificationUrl + "'>Vérifier mon email</a></p>"
                + "<p>Si le lien ne fonctionne pas, copiez et collez l'URL suivante dans votre navigateur :</p>"
                + "<p>" + verificationUrl + "</p>"
                + "<p>Votre code de vérification est : " + code + "</p>";

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

        System.out.println("Debug des vars d'env : SMTP_HOST=" + SMTP_HOST + ", SMTP_PORT=" + SMTP_PORT + ", USERNAME=" + USERNAME + ", PASSWORD=" + PASSWORD + ", BASE_URL=" + BASE_URL);

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
}