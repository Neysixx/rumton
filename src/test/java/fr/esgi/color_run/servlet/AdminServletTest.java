package fr.esgi.color_run.servlet;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.service.AdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServletTest {

    // Sous-classe de test pour rendre les méthodes utilitaires et HTTP publiques/mockables
    static class TestableAdminServlet extends AdminServlet {
        boolean renderTemplateCalled = false;
        boolean renderErrorCalled = false;
        Admin authenticatedAdmin = null;
        @Override
        public void renderError(HttpServletRequest req, HttpServletResponse resp, String msg) { renderErrorCalled = true; }
        @Override
        public void renderTemplate(HttpServletRequest req, HttpServletResponse resp, String tpl, Context ctx) { renderTemplateCalled = true; }
        @Override
        public boolean isAuthenticated(HttpServletRequest req, HttpServletResponse resp) { return true; }
        @Override
        public Admin getAuthenticatedAdmin(HttpServletRequest req) { return authenticatedAdmin; }
        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { super.doGet(req, resp); }
        @Override
        public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { super.doPost(req, resp); }
        @Override
        public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { super.doPut(req, resp); }
        @Override
        public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { super.doDelete(req, resp); }
    }

    private AdminServlet servlet;
    private AdminService adminService;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new TestableAdminServlet();
        adminService = mock(AdminService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        responseWriter = new StringWriter();

        // Injection du service mocké dans le champ privé de la servlet via la réflexion
        var field = AdminServlet.class.getDeclaredField("adminService");
        field.setAccessible(true);
        field.set(servlet, adminService);

        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void testDoGetListAdmins() throws Exception {
        ((TestableAdminServlet)servlet).renderTemplateCalled = false;
        when(request.getPathInfo()).thenReturn(null);
        when(adminService.getAllAdmins()).thenReturn(List.of(Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build()));

        servlet.doGet(request, response);

        assertTrue(((TestableAdminServlet)servlet).renderTemplateCalled);
    }

    @Test
    void testDoGetAdminByIdFound() throws Exception {
        ((TestableAdminServlet)servlet).renderTemplateCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);

        servlet.doGet(request, response);

        assertTrue(((TestableAdminServlet)servlet).renderTemplateCalled);
    }

    @Test
    void testDoGetAdminByIdNotFound() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/99");
        when(adminService.getAdminById(99)).thenReturn(null);

        servlet.doGet(request, response);

        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoGetInvalidId() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/abc");
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        servlet.doGet(request, response);

        assertTrue(sw.toString().contains("Format d'ID invalide") || ((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoGetUnauthenticated() throws Exception {
        // On override isAuthenticated pour ce test
        AdminServlet servletUnauth = new TestableAdminServlet() {
            @Override
            public boolean isAuthenticated(HttpServletRequest req, HttpServletResponse resp) { return false; }
        };
        var field = AdminServlet.class.getDeclaredField("adminService");
        field.setAccessible(true);
        field.set(servletUnauth, adminService);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        ((TestableAdminServlet)servletUnauth).renderTemplateCalled = false;
        servletUnauth.doGet(request, response);
        assertFalse(((TestableAdminServlet)servletUnauth).renderTemplateCalled);
    }

    @Test
    void testDoPostSuccess() throws Exception {
        when(request.getParameter("nom")).thenReturn("Nom");
        when(request.getParameter("prenom")).thenReturn("Prenom");
        when(request.getParameter("email")).thenReturn("mail@mail.com");
        when(request.getParameter("motDePasse")).thenReturn("pass");
        when(adminService.existsByEmail("mail@mail.com")).thenReturn(false);

        servlet.doPost(request, response);

        verify(adminService).createAdmin(any(Admin.class));
        assertTrue(responseWriter.toString().contains("créé avec succès"));
    }

    @Test
    void testDoPostMissingFields() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getParameter("nom")).thenReturn(null);

        servlet.doPost(request, response);

        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoPostEmailExists() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getParameter("nom")).thenReturn("Nom");
        when(request.getParameter("prenom")).thenReturn("Prenom");
        when(request.getParameter("email")).thenReturn("mail@mail.com");
        when(request.getParameter("motDePasse")).thenReturn("pass");
        when(adminService.existsByEmail("mail@mail.com")).thenReturn(true);

        servlet.doPost(request, response);

        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoPostException() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getParameter("nom")).thenReturn("Nom");
        when(request.getParameter("prenom")).thenReturn("Prenom");
        when(request.getParameter("email")).thenReturn("mail@mail.com");
        when(request.getParameter("motDePasse")).thenReturn("pass");
        when(adminService.existsByEmail("mail@mail.com")).thenThrow(new RuntimeException("Erreur"));

        try {
            servlet.doPost(request, response);
        } catch (RuntimeException e) {
            // On attend une exception, mais on veut aussi vérifier le flag
        }
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoPutSuccess() throws Exception {
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);
        when(request.getParameter("nom")).thenReturn("NouveauNom");
        when(request.getParameter("prenom")).thenReturn("NouveauPrenom");
        when(request.getParameter("email")).thenReturn("nouveau@mail.com");
        when(request.getParameter("motDePasse")).thenReturn("newpass");
        when(adminService.existsByEmail("nouveau@mail.com")).thenReturn(false);
        ((TestableAdminServlet)servlet).authenticatedAdmin = null;
        servlet.doPut(request, response);
        verify(adminService).updateAdmin(any(Admin.class));
        assertTrue(responseWriter.toString().contains("mis à jour avec succès"));
    }

    @Test
    void testDoPutInvalidId() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/abc");
        servlet.doPut(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoPutAdminNotFound() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        when(adminService.getAdminById(1)).thenReturn(null);
        servlet.doPut(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoPutEmailExists() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);
        when(request.getParameter("email")).thenReturn("existe@mail.com");
        when(adminService.existsByEmail("existe@mail.com")).thenReturn(true);
        ((TestableAdminServlet)servlet).authenticatedAdmin = null;
        servlet.doPut(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoPutSelfModification() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);
        Admin currentAdmin = Admin.builder().idAdmin(1).build();
        ((TestableAdminServlet)servlet).authenticatedAdmin = currentAdmin;
        servlet.doPut(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoPutException() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);
        when(request.getParameter("nom")).thenReturn("NouveauNom");
        doThrow(new RuntimeException("Erreur")).when(adminService).updateAdmin(any(Admin.class));
        ((TestableAdminServlet)servlet).authenticatedAdmin = null;
        servlet.doPut(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoDeleteSuccess() throws Exception {
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);
        when(adminService.deleteAdmin(1)).thenReturn(true);
        ((TestableAdminServlet)servlet).authenticatedAdmin = null;
        servlet.doDelete(request, response);
        assertTrue(responseWriter.toString().contains("supprimé avec succès"));
    }

    @Test
    void testDoDeleteInvalidId() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/abc");
        servlet.doDelete(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoDeleteAdminNotFound() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        when(adminService.getAdminById(1)).thenReturn(null);
        servlet.doDelete(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoDeleteSelf() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);
        Admin currentAdmin = Admin.builder().idAdmin(1).build();
        ((TestableAdminServlet)servlet).authenticatedAdmin = currentAdmin;
        servlet.doDelete(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoDeleteEchecSuppression() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);
        when(adminService.deleteAdmin(1)).thenReturn(false);
        ((TestableAdminServlet)servlet).authenticatedAdmin = null;
        servlet.doDelete(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }

    @Test
    void testDoDeleteException() throws Exception {
        ((TestableAdminServlet)servlet).renderErrorCalled = false;
        when(request.getPathInfo()).thenReturn("/1");
        Admin admin = Admin.builder().idAdmin(1).nom("A").prenom("B").email("a@b.com").motDePasse("pass").build();
        when(adminService.getAdminById(1)).thenReturn(admin);
        doThrow(new RuntimeException("Erreur")).when(adminService).deleteAdmin(1);
        ((TestableAdminServlet)servlet).authenticatedAdmin = null;
        servlet.doDelete(request, response);
        assertTrue(((TestableAdminServlet)servlet).renderErrorCalled);
    }
}