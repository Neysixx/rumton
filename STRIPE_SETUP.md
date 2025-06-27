# Configuration Stripe pour Color Run

## Prérequis

1. Créer un compte Stripe sur [https://stripe.com](https://stripe.com)
2. Activer le mode test dans votre dashboard Stripe

## 1. Récupération des clés API

### Dans votre dashboard Stripe :

1. **Accédez aux clés API** :
   - Connectez-vous à votre [dashboard Stripe](https://dashboard.stripe.com)
   - Naviguez vers `Développeurs > Clés API`

2. **Récupérez vos clés de test** :
   - **Clé publique test** : `pk_test_...`
   - **Clé secrète test** : `sk_test_...`

## 2. Configuration de l'application

### Étape 1 : Configuration des propriétés

1. Copiez le fichier `application.properties.example` vers `application.properties` :
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

2. Modifiez le fichier `application.properties` et remplacez les valeurs Stripe :
```properties
# Stripe Configuration (Test Mode)
stripe.secret.key=sk_test_VOTRE_CLE_SECRETE_ICI
```

### Étape 2 : Rebuild de l'application

```bash
mvn clean install
```

## 3. Test de l'intégration

### Cartes de test Stripe

Utilisez ces numéros de carte pour tester les paiements :

- **Paiement réussi** : `4242 4242 4242 4242`
- **Paiement refusé** : `4000 0000 0000 0002`
- **Authentification 3D Secure** : `4000 0025 0000 3155`

**Informations complémentaires pour les tests** :
- Date d'expiration : n'importe quelle date future (ex: 12/34)
- CVC : n'importe quel nombre à 3 chiffres (ex: 123)
- Code postal : n'importe quel code postal valide

### Flux de test

1. **Créer une course payante** (prix > 0€)
2. **Tenter une inscription** en tant que participant
3. **Être redirigé vers Stripe Checkout**
4. **Utiliser une carte de test** pour confirmer le paiement
5. **Vérifier l'inscription** dans la liste des participants

## 5. Monitoring et logs

### Dans l'application

Les logs Stripe apparaissent dans la console :
```
INFO: Produit Stripe créé avec succès: prod_XXXXXX
INFO: Session de paiement créée: cs_test_XXXXXX
INFO: Paiement complété pour la session: cs_test_XXXXXX
```

### Dans le dashboard Stripe

1. **Événements** : `Développeurs > Événements`
2. **Paiements** : `Paiements > Vue d'ensemble`
3. **Produits** : `Catalogue de produits`

## 4. Fonctionnalités implémentées

### ✅ Gestion automatique des produits Stripe

- **Création de course** → Création d'un produit Stripe (si prix > 0€)
- **Modification de course** → Mise à jour du produit Stripe
- **Suppression de course** → Désactivation du produit Stripe

### ✅ Processus de paiement

- **Interface sécurisée** avec Stripe Checkout
- **Validation du paiement** avant inscription
- **Gestion des erreurs** et cas d'edge
- **Pages de succès/échec** personnalisées

### ✅ Sécurité

- **Aucune donnée bancaire** stockée sur vos serveurs
- **Vérification des paiements** côté serveur
- **Protection contre les inscriptions multiples**

## 5. Architecture technique

```
CourseServlet
├── Création course → StripeService.createProductForCourse()
├── Modification course → StripeService.updateProduct()
└── Suppression course → StripeService.deleteProduct()

PaymentServlet
├── /payment/{courseId} → StripeService.createCheckoutSession()
├── /payment-success → StripeService.isPaymentCompleted()
└── /payment-cancel → Page d'annulation

StripeService
├── Gestion des produits Stripe
├── Création des sessions de paiement
└── Vérification des paiements
```

## 6. Dépannage

### Erreur "Invalid API key"
- Vérifiez que vous utilisez la bonne clé secrète test (`sk_test_...`)
- Vérifiez que la clé est correctement définie dans `application.properties`

### Erreur "Product not found"
- Vérifiez que la course a bien un `stripe_product_id` en base de données
- Recréez le produit en modifiant la course

### Paiement non confirmé
- Vérifiez les logs Stripe dans le dashboard
- Vérifiez que vous utilisez une carte de test valide

## 7. Passage en production

⚠️ **Important** : Pour passer en production :

1. **Activer votre compte Stripe** (vérification d'identité requise)
2. **Remplacer les clés test par les clés live** :
   - `pk_live_...` pour la clé publique
   - `sk_live_...` pour la clé secrète
3. **Mettre à jour les webhooks** avec votre domaine de production
4. **Tester avec de vraies cartes** (petits montants)

---

## Support

Pour toute question relative à cette intégration Stripe, consultez :
- [Documentation Stripe](https://stripe.com/docs)
- [API Reference Stripe Java](https://stripe.com/docs/api?lang=java)
- [Guide Stripe Checkout](https://stripe.com/docs/checkout) 