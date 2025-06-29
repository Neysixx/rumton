package fr.esgi.color_run.service;

import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.Context;

/**
 * Interface pour le service de genération et d'envoie de PDF
 */
public interface PdfService {
    void generatePdf(HttpServletResponse response, String templateName, Context context, String fileName);
}
