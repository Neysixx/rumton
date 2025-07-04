-- Script de seed pour la base de données

-- Insertion des ADMIN
INSERT INTO ADMIN (id_admin, nom, prenom, email, mot_de_passe, url_profile) VALUES
    (1, 'MOUTON', 'Corentin', 'admin@rumton.fr', '$2a$10$jMwSgK171G1E0hDB2wUXEOMM1cahY271MdkoWQrT4nBm3mS1UOB6C', '/color_run_war_exploded/uploads/f5e519fc-a7b5-4d63-a31d-92c433fe4809.png');

-- Insertion des CAUSE
INSERT INTO CAUSE (id_cause, intitule) VALUES
                                           (1, 'Lutte contre le cancer'),
                                           (2, 'Protection de l''environnement'),
                                           (3, 'Aide aux personnes en situation de handicap'),
                                           (4, 'Éducation pour tous'),
                                           (5, 'Lutte contre la pauvreté'),
                                           (6, 'Protection animale');

-- Insertion des PARTICIPANT
INSERT INTO PARTICIPANT (id_participant, nom, prenom, email, mot_de_passe, url_profile, est_organisateur, date_creation, est_verifie) VALUES
                                                                                                                                          (1, 'SENRENS', 'Kyllian', 'kylliansenrens3004@gmail.com', '$2a$10$jMwSgK171G1E0hDB2wUXEOMM1cahY271MdkoWQrT4nBm3mS1UOB6C', '', false, '2025-07-01 12:33:29.120141', true),
                                                                                                                                          (2, 'LAVAYSSIERE', 'Romain', 'romain.lavayssiere63@gmail.com', '$2a$10$jMwSgK171G1E0hDB2wUXEOMM1cahY271MdkoWQrT4nBm3mS1UOB6C', '', false, '2025-07-01 12:33:29.120141', true),
                                                                                                                                          (3, 'PELUD', 'Tom', 'pelud.tom@gmail.com', '$2a$10$jMwSgK171G1E0hDB2wUXEOMM1cahY271MdkoWQrT4nBm3mS1UOB6C', '', true, '2025-07-01 12:33:29.120641', true);

-- Insertion des COURSE
INSERT INTO COURSE (id_course, id_cause, id_organisateur, nom, description, date_depart, ville, code_postal, adresse, distance, max_participants, prix_participation, lat, lon, obstacles, stripe_product_id) VALUES
                                                                                                                                                                                                                  (1, 1, 3, 'Course Rose Lyon', 'Course caritative pour soutenir la recherche contre le cancer du sein. Parcours urbain traversant les plus beaux quartiers de Lyon avec départ depuis la Place Bellecour.', '2025-10-15 10:10:00.000000', 'Lyon', 69002, '2 Place Bellecour', 10.5, 800, 20, 45.758827209472656, 4.831127166748047, 'true', 'prod_SbG54sQ5l1mHgL'),
                                                                                                                                                                                                                  (3, NULL, 3, 'Solidarité Run Paris', 'Course urbaine dans Paris pour collecter des fonds en faveur des personnes en situation de handicap. Parcours accessible et festif dans le centre historique.', '2025-12-11 10:00:00.000000', 'Paris', 75004, 'Place des Vosges', 5, 1200, 15, 48.85514450073242, 2.3653509616851807, 'false', 'prod_SbGB72APOXBAVv'),
                                                                                                                                                                                                                  (4, 2, 3, 'Trail Vert Annecy', 'Course écologique autour du lac d''Annecy pour sensibiliser à la protection de l''environnement. Parcours nature avec vue imprenable sur les montagnes.', '2025-09-20 08:30:00.000000', 'Annecy', 74600, 'Parc Charles Bosson (Annecy)', 15, 400, 35, 45.90398025512695, 6.141674995422363, 'false', 'prod_SbHokEBPXkDlAH');

-- Insertion des PARTICIPATION
INSERT INTO PARTICIPATION (id_participation, id_participant, id_course, numero_dossard, date_reservation) VALUES
    (4, 1, 4, 1, '2025-07-01 16:33:29.439000');

-- Insertion des MESSAGE
INSERT INTO MESSAGE (id_message, id_emetteur, id_course, id_message_parent, contenu, date_publication) VALUES
    (3, 1, 4, NULL, 'Bonjour. J''ai déjà participé à cette course elle est top. Mais pensez à prendre votre crème solaire !! 😊', '2025-07-01 16:35:12.343000');