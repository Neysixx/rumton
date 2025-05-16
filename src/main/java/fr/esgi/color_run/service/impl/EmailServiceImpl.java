package fr.esgi.color_run.service.impl;

     import fr.esgi.color_run.service.EmailService;
     import fr.esgi.color_run.util.DebugUtil;

     import javax.mail.*;
     import javax.mail.internet.InternetAddress;
     import javax.mail.internet.MimeMessage;
     import java.util.Properties;
     import java.util.UUID;

     public class EmailServiceImpl implements EmailService {
         private static final String SMTP_HOST = "smtp.gmail.com";
         private static final String SMTP_PORT = "587";
         private static final String USERNAME = "votre-email@gmail.com";
         private static final String PASSWORD = "votre-mot-de-passe";
         private static final String BASE_URL = "http://votre-domaine.com";

         @Override
         public String envoyerEmailVerification(String email) throws MessagingException {
             if (email == null || email.trim().isEmpty()) {
                 throw new IllegalArgumentException("L'email ne peut pas être null ou vide");
             }

             String token = UUID.randomUUID().toString();
             String verificationUrl = BASE_URL + "/verify?email=" + email + "&token=" + token;

             // Corps du message en HTML
             String htmlContent = "<h1>Vérification de votre adresse email</h1>"
                     + "<p>Merci de vous être inscrit. Veuillez cliquer sur le lien ci-dessous pour vérifier votre adresse email :</p>"
                     + "<p><a href='" + verificationUrl + "'>Vérifier mon email</a></p>"
                     + "<p>Si le lien ne fonctionne pas, copiez et collez l'URL suivante dans votre navigateur :</p>"
                     + "<p>" + verificationUrl + "</p>";

             envoyerEmail(email, "Vérification de votre adresse email", htmlContent);

             return token;
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
     }