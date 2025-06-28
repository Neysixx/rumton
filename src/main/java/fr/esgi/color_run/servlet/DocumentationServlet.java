package fr.esgi.color_run.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

import java.io.IOException;

/**
 * Servlet qui gère la documentaion de l'app "les footers etc"
 */
@WebServlet(name = "documentationServlet", value = {"/faq", "/about-us", "/contact", "/legal-mentions"})
public class DocumentationServlet extends BaseWebServlet{

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Création du contexte Thymeleaf
        Context context = new Context();

        switch (request.getServletPath()) {
            case "/faq":
                renderTemplate(request, response, "doc/faq", context);
                break;
            case "/about-us":
                renderTemplate(request, response, "doc/aboutUs", context);
                break;
            case "/contact":
                renderTemplate(request, response, "doc/contact", context);
                break;
            case "/legal-mentions":
                renderTemplate(request, response, "doc/legalMentions", context);
                break;


        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/");
    }
}
