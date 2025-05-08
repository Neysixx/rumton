package fr.esgi.color_run.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

/**
 * Servlet qui gère l'inscription des nouveaux participants
 */
@WebServlet(name = "homeServlet", value = "")
public class HomeServlet extends HttpServlet {

    /**
     * Affiche le formulaire d'inscription
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Récupération du moteur de template
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");

        // Création du contexte Thymeleaf
        Context context = new Context();

        // Traitement de la page
        templateEngine.process("home", context, response.getWriter());
    }
}
