# 🎨 Rumton – Plateforme de courses colorées en France

Bienvenue sur **Rumton**, l’application web qui permet de créer, organiser et participer à des courses colorées à travers toute la France, tout en soutenant des causes associatives !

---

## 👨‍💻 Créateurs

- **Romain LAVAYSSIERE**
- **Kyllian SENRENS**
- **Tom PELUD**

Projet fictif réalisé dans le cadre d’un cursus à l’ESGI.

---

## 🚀 Présentation

Rumton est une plateforme festive et solidaire où chaque utilisateur peut :

- S’inscrire à des courses colorées partout en France
- Créer sa propre course et choisir une cause à soutenir
- Gérer ses inscriptions et télécharger son dossard personnalisé
- Découvrir et soutenir des causes associatives

Toutes les courses sont payantes, une partie des fonds est reversée à la cause choisie par l’organisateur.

---

## 🛠️ Stack technique

- **Java 21+**
- **Jakarta Servlet**
- **Thymeleaf**
- **Maven**
- **H2 Database**
- **CSS3 / Responsive Design**
- **OpenPDF (Flying Saucer) pour la génération de PDF**
- **JWT pour l’authentification**
- **SMTP pour l’envoi d’emails**

---

## 📦 Installation

1. **Cloner le projet**

   ```bash
   git clone https://github.com/Neysixx/rumton.git
   cd rumton
   ```

2. **Configurer la base de données et les variables d’environnement**

   - Voir `src/main/resources/application.properties`
   - Dupliquer ce ficher dans `src/test/ressources`

3. **Installer les outils nécessaires**

   - Installer Tomcat
   - Installer h2

4. **Lancer le projet**
   - lancer le script script(1) pour setup la base de donnée ( application / test )
   - créer une launch configuration avec Tomcat local, dans la section Deployment ajouter l'artifact "color_run:war exploded"
   - vous pouvez maintenant lancer le projet

---

## 🌐 Routes principales

| Route                           | Méthode  | Description                                                            |
| ------------------------------- | -------- | ---------------------------------------------------------------------- |
| `/`                             | GET      | Accueil, présentation du concept, dernières courses                    |
| `/register`                     | GET/POST | Inscription d’un nouvel utilisateur                                    |
| `/login`                        | GET/POST | Connexion                                                              |
| `/logout`                       | GET      | Déconnexion                                                            |
| `/courses`                      | GET      | Liste de toutes les courses                                            |
| `/courses/{id}`                 | GET      | Détail d’une course, liste des participants, téléchargement du dossard |
| `/courses-create`               | GET/POST | Création d’une nouvelle course (organisateur uniquement)               |
| `/courses-edit/{id}`            | GET/POST | Modification d’une course (organisateur uniquement)                    |
| `/causes`                       | GET      | Liste des causes associatives                                          |
| `/causes-create`                | GET/POST | Création d’une cause (admin uniquement)                                |
| `/participations`               | POST     | Inscription à une course (génère un dossard PDF, envoie un mail)       |
| `/participations/{id}/dossard`  | GET      | Téléchargement du dossard PDF pour une participation                   |
| `/profile`                      | GET      | Profil utilisateur                                                     |
| `/profile/edit`                 | GET/POST | Modification du profil                                                 |
| `/profile/demande-organisateur` | GET/POST | Demande pour devenir organisateur                                      |
| `/admin/users`                  | GET      | Liste des utilisateurs (admin)                                         |
| `/admin/users/{id}/edit`        | GET/POST | Modification d’un utilisateur (admin)                                  |
| `/admin/demandes`               | GET      | Liste des demandes pour devenir organisateur (admin)                   |
| `/faq`                          | GET      | Foire aux questions                                                    |
| `/about-us`                     | GET      | À propos de l’équipe et du projet                                      |
| `/contact`                      | GET/POST | Page de contact                                                        |
| `/legal-mentions`               | GET      | Mentions légales                                                       |
| `/mot-de-passe-oublie`          | GET/POST | Réinitialisation du mot de passe                                       |

---

## 📄 Fonctionnalités principales

- **Inscription/Connexion sécurisée** (JWT)
- **Création et gestion de courses** (pour les organisateurs)
- **Inscription à une course** (avec génération et téléchargement du dossard PDF)
- **Messagerie dans l'espace course**
- **Gestion des causes associatives**
- **Espace profil** (édition, demande pour devenir organisateur)
- **Administration** (gestion des utilisateurs, validation des demandes organisateur)
- **FAQ, mentions légales, contact, à propos**
- **Responsive design** pour une expérience optimale sur tous supports

---

## ⚠️ Remarque

Ce projet est fictif et réalisé à des fins pédagogiques.  
Aucune donnée réelle n’est exploitée, et les événements/courses sont simulés.

---

## 🎉 Amusez-vous bien sur Rumton !

---

2025 © Rumton – Romain LAVAYSSIERE, Kyllian SENRENS, Tom PELUD
