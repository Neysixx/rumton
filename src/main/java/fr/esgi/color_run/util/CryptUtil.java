package fr.esgi.color_run.util;

    import org.mindrot.jbcrypt.BCrypt;

    /**
     * Classe utilitaire pour le chiffrement des mots de passe
     */
    public class CryptUtil {

        /**
         * Chiffre un mot de passe en utilisant BCrypt
         * @param password Le mot de passe en clair à chiffrer
         * @return Le mot de passe chiffré
         */
        public static String hashPassword(String password) {
            return BCrypt.hashpw(password, BCrypt.gensalt());
        }

        /**
         * Vérifie si un mot de passe en clair correspond à un mot de passe chiffré
         * @param password Le mot de passe en clair à vérifier
         * @param hashedPassword Le mot de passe chiffré à comparer
         * @return true si les mots de passe correspondent, false sinon
         */
        public static boolean checkPassword(String password, String hashedPassword) {
            return BCrypt.checkpw(password, hashedPassword);
        }
    }