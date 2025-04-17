package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.service.CauseService;
import fr.esgi.color_run.service.impl.CauseServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

/**
 * Servlet de gestion des causes
 */
@WebServlet(name = "causeServlet", value = {"/causes", "/api/causes"})
public class CauseServlet extends HttpServlet {

    public void init() {
        // initialisation si nécessaire
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, IOException {
        // Récupération du moteur de template
        TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");

        // Préparation du contexte Thymeleaf
        Context context = new Context();

        // Traitement de la vue (liste des causes ou formulaire de création)
        templateEngine.process("causes", context, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CauseService causeService = new CauseServiceImpl();

        try {
            // Récupération du champ du formulaire
            String intitule = request.getParameter("intitule");

            // Validation
            if (intitule == null || intitule.trim().isEmpty()) {
                throw new IllegalArgumentException("L'intitulé de la cause est obligatoire");
            }

            // Création de l'objet Cause
            Cause cause = Cause.builder()
                    .intitule(intitule.trim())
                    .build();

            // Enregistrement
            causeService.createCause(cause);

            // Redirection après succès
            response.sendRedirect(request.getContextPath() + "/causes");

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Erreur lors de la création de la cause : " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/templates/causes.html").forward(request, response);
        }
    }
}
