package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;
import java.io.OutputStream;

public class PdfServiceImpl implements PdfService {

    private final TemplateEngine templateEngine;

    public PdfServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public void generatePdf(HttpServletResponse response, String templateName, Context context, String fileName) {
        // Générer le HTML avec Thymeleaf
        String htmlContent = templateEngine.process(templateName, context);

        // Configurer la réponse HTTP
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // Convertir le HTML en PDF et écrire dans la réponse
        try (OutputStream os = response.getOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(os, false);
            renderer.finishPDF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
