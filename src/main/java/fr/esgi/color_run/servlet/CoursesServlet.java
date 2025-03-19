package fr.esgi.color_run.servlet;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

//import fr.esgi.color_run.service.CourseService;
//import fr.esgi.color_run.service.impl.CourseServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

/**
 * Cette classe aura le pouvoir de traiter des requêtes HTTP
 */
//@WebServlet(name = "coursesServlet", value = {"/index", "/courses"})
//public class CoursesServlet extends HttpServlet {
//    private String message;
//
//    private CourseService courseService = new CourseServiceImpl();
//
//    public void init() {
//        message = "Liste des courses";
//    }
//
//    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//        // On récupère le moteur de template dans le contexte des servlets
//        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
//
//        // On crée un context Thymeleaf qui va accueille des objets Java
//        // qui seront envoyés à la vue Thymeleaf
//        Context context = new Context();
//        context.setVariable("courses", courseService.recupererCourses());
//
//        // On invoque la méthode process qui formule la réponse qui sera renvoyée au navigateur
//        templateEngine.process("courses", context, response.getWriter());
//    }
//
//}
