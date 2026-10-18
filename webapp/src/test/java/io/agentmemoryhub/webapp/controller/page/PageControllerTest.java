package io.agentmemoryhub.webapp.controller.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agentmemoryhub.core.common.PlatformRole;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.webapp.controller.page.admin.AdminUserPageController;
import io.agentmemoryhub.webapp.controller.page.project.ProjectPageController;
import io.agentmemoryhub.webapp.handler.session.WebAppSessionHandler;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

class PageControllerTest {
    private final WebAppSessionHandler sessionHandler = new WebAppSessionHandler();

    @Test
    void showsProjectDashboardOnlyForAuthenticatedSessions() {
        // Verifies the dashboard redirects anonymous sessions and exposes the signed-in user to its view.
        var controller = new ProjectPageController(sessionHandler);
        var session = new MockHttpSession();
        var model = new ExtendedModelMap();

        assertEquals("redirect:/login", controller.getProjectsPage(session, model));
        var userRecord = new UserRecord("user-1", "Test User", "user@example.com", PlatformRole.USER, true, Instant.now(), 0L);
        sessionHandler.storeUser(session, userRecord);

        assertEquals("projects", controller.getProjectsPage(session, model));
        assertEquals("user-1", model.getAttribute("userId"));
        org.junit.jupiter.api.Assertions.assertNotNull(model.getAttribute("csrfToken"));
    }

    @Test
    void restrictsTheUserAdministrationPageToAdministrators() {
        // Verifies user administration pages require an admin role and generate a CSRF token.
        var controller = new AdminUserPageController(sessionHandler);
        var session = new MockHttpSession();
        var model = new ExtendedModelMap();
        assertEquals("redirect:/login", controller.getUsersPage(session, model));

        sessionHandler.storeUser(session, new UserRecord("user-1", "User", "user@example.com", PlatformRole.USER, true, null, 0L));
        assertEquals("redirect:/dashboard", controller.getUsersPage(session, model));

        sessionHandler.storeUser(session, new UserRecord("admin-1", "Admin", "admin@example.com", PlatformRole.ADMIN, true, null, 0L));
        assertEquals("users", controller.getUsersPage(session, model));
        org.junit.jupiter.api.Assertions.assertNotNull(model.getAttribute("csrfToken"));
    }
}
