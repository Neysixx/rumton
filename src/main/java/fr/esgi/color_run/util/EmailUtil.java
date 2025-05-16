package fr.esgi.color_run.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;

public class EmailUtil {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "votre-email@gmail.com";
    private static final String PASSWORD = "votre-mot-de-passe";

    public static String sendVerificationEmail(String toEmail) throws MessagingException {
        String token = UUID.randomUUID().toString();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(USERNAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Vérification de votre adresse email");

        String verificationUrl = "http://votre-domaine.com/verify?email=" + toEmail;

        // Corps du message en HTML
        String htmlContent = "<h1>Vérification de votre adresse email</h1>"
                + "<p>Merci de vous être inscrit. Veuillez cliquer sur le lien ci-dessous pour vérifier votre adresse email :</p>"
                + "<p><a href='" + verificationUrl + "'>Vérifier mon email</a></p>"
                + "<p>Si le lien ne fonctionne pas, copiez et collez l'URL suivante dans votre navigateur :</p>"
                + "<p>" + verificationUrl + "</p>";

        message.setContent(htmlContent, "text/html; charset=utf-8");

        Transport.send(message);
        return token;
    }
}