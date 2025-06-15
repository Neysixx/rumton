package fr.esgi.color_run.servlet;

import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.impl.CauseServiceImpl;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
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
public class HomeServlet extends BaseWebServlet {

    private CourseService courseService;

    @Override
    public void init() {
        super.init();
        courseService = new CourseServiceImpl();
    }

    /**
     * Affiche le formulaire d'inscription
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Création du contexte Thymeleaf
        Context context = new Context();
        context.setVariable("courses", courseService.getRecentCourses(3));

        // Rendu de la page
        renderTemplate(request, response, "home", context);
    }
}
