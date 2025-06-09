package fr.esgi.color_run.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    public static final String JWT_COOKIE_NAME = "jwt_token";
    public static final int JWT_COOKIE_MAX_AGE = 24 * 60 * 60; // 24 heures en secondes

    /**
     * Définit un cookie
     * @param response La réponse HTTP
     * @param name Le nom du cookie
     * @param value La valeur du cookie
     * @param maxAge La durée de vie du cookie en secondes
     */
    public static void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    /**
     * Récupère un cookie
     * @param request La requête HTTP
     * @param name Le nom du cookie
     * @return La valeur du cookie
     */
    public static String getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Supprime un cookie
     * @param response La réponse HTTP
     * @param name Le nom du cookie
     */
    public static void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null); 
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}

