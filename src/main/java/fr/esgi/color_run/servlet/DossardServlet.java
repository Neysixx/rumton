package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Participation;
import fr.esgi.color_run.service.ParticipationService;
import fr.esgi.color_run.service.PdfService;
import fr.esgi.color_run.service.impl.ParticipationServiceImpl;
import fr.esgi.color_run.service.impl.PdfServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

@WebServlet(name = "dossardServlet", value = "/dossard/*")
public class DossardServlet extends BaseWebServlet {

    private ParticipationService participationService;
    private PdfService pdfService;

    public DossardServlet() {
        super.init();
        this.participationService = new ParticipationServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            int participationId = Integer.parseInt(pathInfo.substring(1));
            participationService.getParticipationById(participationId)
                    .ifPresentOrElse(
                            participation -> {
                                // Génération du dossard en PDF
                                TemplateEngine templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
                                this.pdfService = new PdfServiceImpl(templateEngine);
                                Context pdfContext = new Context();
                                pdfContext.setVariable("participant", participation.getParticipant());
                                pdfContext.setVariable("course", participation.getCourse());
                                pdfContext.setVariable("participation", participation);

                                pdfService.generatePdf(response, "pdf/dossard", pdfContext, "dossard.pdf");
                            },
                            () -> {
                                try {
                                    renderError(request, response,
                                            "Participation non trouvée avec l'ID " + participationId);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ServletException e) {
                                    throw new RuntimeException(e);
                                }
                            });
        }
    }
}
