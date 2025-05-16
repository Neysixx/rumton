package fr.esgi.color_run.service;

import javax.mail.MessagingException;

public interface EmailService {

    /**
     * Envoie un email de vérification à l'adresse spécifiée
     * @param email L'adresse email du destinataire
     * @return Le token de vérification généré
     * @throws MessagingException Si une erreur survient lors de l'envoi de l'email
     */
    String envoyerEmailVerification(String email) throws MessagingException;

    /**
     * Envoie un email avec un contenu et un sujet personnalisés
     * @param destinataire L'adresse email du destinataire
     * @param sujet Le sujet de l'email
     * @param contenu Le contenu HTML de l'email
     * @throws MessagingException Si une erreur survient lors de l'envoi de l'email
     */
    void envoyerEmail(String destinataire, String sujet, String contenu) throws MessagingException;
}