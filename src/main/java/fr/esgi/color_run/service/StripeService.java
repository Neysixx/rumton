package fr.esgi.color_run.service;

import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import fr.esgi.color_run.business.Course;

public interface StripeService {
    /**
     * Crée un produit Stripe pour une course
     * @param course La course pour laquelle créer le produit
     * @return L'ID du produit Stripe créé
     */
    String createProductForCourse(Course course);

    /**
     * Met à jour un produit Stripe existant
     * @param stripeProductId L'ID du produit Stripe à mettre à jour
     * @param course La course avec les nouvelles informations
     * @return true si la mise à jour a réussi
     */
    boolean updateProduct(String stripeProductId, Course course);

    /**
     * Met à jour un produit Stripe existant avec un nouveau prix
     * @param stripeProductId L'ID du produit Stripe à mettre à jour
     * @param course La course avec les nouvelles informations
     * @param newPrice Le nouveau prix (si différent de l'ancien)
     * @return true si la mise à jour a réussi
     */
    boolean updateProductWithPrice(String stripeProductId, Course course, float newPrice);

    /**
     * Crée un nouveau prix pour un produit existant
     * @param stripeProductId L'ID du produit Stripe
     * @param newPrice Le nouveau prix
     * @return L'ID du nouveau prix créé
     */
    String createNewPriceForProduct(String stripeProductId, float newPrice);

    /**
     * Supprime un produit Stripe
     * @param stripeProductId L'ID du produit Stripe à supprimer
     * @return true si la suppression a réussi
     */
    boolean deleteProduct(String stripeProductId);

    /**
     * Crée une session de paiement Stripe Checkout
     * @param course La course pour laquelle créer la session
     * @param participantEmail L'email du participant
     * @param successUrl L'URL de retour en cas de succès
     * @param cancelUrl L'URL de retour en cas d'annulation
     * @return L'URL de la session de paiement
     */
    String createCheckoutSession(Course course, String participantEmail, String successUrl, String cancelUrl);

    /**
     * Vérifie si le paiement d'une session a été complété
     * @param sessionId L'ID de la session de paiement
     * @return true si le paiement est complété
     */
    boolean isPaymentCompleted(String sessionId);
} 