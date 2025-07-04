package fr.esgi.color_run.service.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Product;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.service.StripeService;
import fr.esgi.color_run.util.PropertiesUtil;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class StripeServiceImpl implements StripeService {

    public StripeServiceImpl() {
        // Configuration de la clé secrète Stripe
        Stripe.apiKey = PropertiesUtil.getProperty("stripe.secret.key");
    }

    @Override
    public String createProductForCourse(Course course) {
        System.out.println("Création du produit Stripe pour la course: " + course.getNom());
        System.out.println("Clé secrète Stripe chargée: " + Stripe.apiKey);
        try {
            // Formater la date de la course
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
            String formattedDate = dateFormat.format(course.getDateDepart());

            // Métadonnées pour lier le produit à la course
            Map<String, String> metadata = new HashMap<>();
            metadata.put("course_id", String.valueOf(course.getIdCourse()));
            metadata.put("course_name", course.getNom());
            metadata.put("course_date", formattedDate);

            // Création du produit Stripe
            ProductCreateParams params = ProductCreateParams.builder()
                    .setName("Course Color Run - " + course.getNom())
                    .setDescription(String.format(
                            "Participation à la course '%s' le %s à %s (%s km)",
                            course.getNom(),
                            formattedDate,
                            course.getVille(),
                            course.getDistance()
                    ))
                    .putAllMetadata(metadata)
                    .setActive(true)
                    .build();

            Product product = Product.create(params);

            // Création du prix associé (en centimes)
            long priceInCents = Math.round(course.getPrixParticipation() * 100);
            
            PriceCreateParams priceParams = PriceCreateParams.builder()
                    .setProduct(product.getId())
                    .setUnitAmount(priceInCents)
                    .setCurrency("eur")
                    .build();

            Price.create(priceParams);

            System.out.println("Produit Stripe créé avec succès: " + product.getId());
            return product.getId();

        } catch (StripeException e) {
            System.err.println("Erreur lors de la création du produit Stripe: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean updateProduct(String stripeProductId, Course course) {
        try {
            // Formater la date de la course
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
            String formattedDate = dateFormat.format(course.getDateDepart());

            // Métadonnées mises à jour
            Map<String, String> metadata = new HashMap<>();
            metadata.put("course_id", String.valueOf(course.getIdCourse()));
            metadata.put("course_name", course.getNom());
            metadata.put("course_date", formattedDate);

            // Mise à jour du produit
            Product product = Product.retrieve(stripeProductId);
            
            ProductUpdateParams params = ProductUpdateParams.builder()
                    .setName("Course Color Run - " + course.getNom())
                    .setDescription(String.format(
                            "Participation à la course '%s' le %s à %s (%s km)",
                            course.getNom(),
                            formattedDate,
                            course.getVille(),
                            course.getDistance()
                    ))
                    .putAllMetadata(metadata)
                    .build();

            product.update(params);

            System.out.println("Produit Stripe mis à jour avec succès: " + stripeProductId);
            return true;

        } catch (StripeException e) {
            System.err.println("Erreur lors de la mise à jour du produit Stripe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateProductWithPrice(String stripeProductId, Course course, float newPrice) {
        try {
            // D'abord mettre à jour les informations du produit
            boolean productUpdated = updateProduct(stripeProductId, course);
            if (!productUpdated) {
                return false;
            }

            // Vérifier si le prix a réellement changé
            String currentActivePriceId = getActivePriceForProduct(stripeProductId);
            if (currentActivePriceId != null) {
                Price currentPrice = Price.retrieve(currentActivePriceId);
                long currentPriceInCents = currentPrice.getUnitAmount();
                long newPriceInCents = Math.round(newPrice * 100);

                // Si le prix n'a pas changé, pas besoin de créer un nouveau prix
                if (currentPriceInCents == newPriceInCents) {
                    System.out.println("Le prix n'a pas changé, pas de mise à jour nécessaire");
                    return true;
                }

                // Désactiver l'ancien prix
                PriceUpdateParams priceUpdateParams = PriceUpdateParams.builder()
                        .setActive(false)
                        .build();
                currentPrice.update(priceUpdateParams);
                System.out.println("Ancien prix désactivé: " + currentActivePriceId);
            }

            // Créer un nouveau prix
            String newPriceId = createNewPriceForProduct(stripeProductId, newPrice);
            if (newPriceId != null) {
                System.out.println("Nouveau prix créé avec succès: " + newPriceId);
                return true;
            } else {
                System.err.println("Erreur lors de la création du nouveau prix");
                return false;
            }

        } catch (StripeException e) {
            System.err.println("Erreur lors de la mise à jour du produit avec nouveau prix: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String createNewPriceForProduct(String stripeProductId, float newPrice) {
        try {
            // Création du nouveau prix (en centimes)
            long priceInCents = Math.round(newPrice * 100);
            
            PriceCreateParams priceParams = PriceCreateParams.builder()
                    .setProduct(stripeProductId)
                    .setUnitAmount(priceInCents)
                    .setCurrency("eur")
                    .setActive(true)
                    .build();

            Price newPriceObj = Price.create(priceParams);
            System.out.println("Nouveau prix créé: " + newPriceObj.getId() + " pour " + newPrice + "€");
            return newPriceObj.getId();

        } catch (StripeException e) {
            System.err.println("Erreur lors de la création du nouveau prix: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean deleteProduct(String stripeProductId) {
        try {
            Product product = Product.retrieve(stripeProductId);
            
            // Désactiver le produit (Stripe ne permet pas de supprimer définitivement)
            ProductUpdateParams params = ProductUpdateParams.builder()
                    .setActive(false)
                    .build();
            
            product.update(params);

            System.out.println("Produit Stripe désactivé avec succès: " + stripeProductId);
            return true;

        } catch (StripeException e) {
            System.err.println("Erreur lors de la suppression du produit Stripe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String createCheckoutSession(Course course, String participantEmail, String successUrl, String cancelUrl) {
        try {
            if (course.getStripeProductId() == null) {
                throw new IllegalArgumentException("La course n'a pas de produit Stripe associé");
            }

            // Récupérer le prix du produit
            Product product = Product.retrieve(course.getStripeProductId());
            
            // Obtenir le premier prix actif du produit
            String priceId = getActivePriceForProduct(course.getStripeProductId());
            if (priceId == null) {
                throw new IllegalArgumentException("Aucun prix actif trouvé pour le produit");
            }

            // Métadonnées pour tracer la session
            Map<String, String> metadata = new HashMap<>();
            metadata.put("course_id", String.valueOf(course.getIdCourse()));
            metadata.put("participant_email", participantEmail);

            // Création de la session de paiement
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setCustomerEmail(participantEmail)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();

            Session session = Session.create(params);

            System.out.println("Session de paiement créée: " + session.getId());
            return session.getUrl();

        } catch (StripeException e) {
            System.err.println("Erreur lors de la création de la session de paiement: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isPaymentCompleted(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return "complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus());
        } catch (StripeException e) {
            System.err.println("Erreur lors de la vérification du paiement: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère l'ID du prix actif pour un produit donné
     */
    private String getActivePriceForProduct(String productId) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("product", productId);
            params.put("active", true);
            params.put("limit", 1);

            var prices = Price.list(params);
            
            if (prices.getData().isEmpty()) {
                return null;
            }
            
            return prices.getData().get(0).getId();
            
        } catch (StripeException e) {
            System.err.println("Erreur lors de la récupération du prix: " + e.getMessage());
            return null;
        }
    }
} 