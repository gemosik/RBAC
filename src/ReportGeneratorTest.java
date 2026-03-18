import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ReportGeneratorTest {

    @Test
    void generateUserReportIncludesUsersAndRoles() {
        RBACSystem system = new RBACSystem();
        system.initialize();

        UserManager um = system.getUserManager();
        RoleManager rm = system.getRoleManager();
        AssignmentManager am = system.getAssignmentManager();

        User john = User.create("john_user", "John User", "john_user@example.com");
        um.add(john);

        Role viewer = rm.findByName("Viewer").orElseThrow();
        am.add(new PermanentAssignment(john, viewer, AssignmentMetadata.now("admin", "test")));

        ReportGenerator rg = new ReportGenerator();
        String report = rg.generateUserReport(um, am);

        assertTrue(report.contains("Отчёт по пользователям"));
        assertTrue(report.contains("john_user"));
        assertTrue(report.contains("Viewer"));
    }

    @Test
    void generateRoleReportShowsCountsInTable() {
        RBACSystem system = new RBACSystem();
        system.initialize();

        ReportGenerator rg = new ReportGenerator();
        String report = rg.generateRoleReport(system.getRoleManager(), system.getAssignmentManager());

        assertTrue(report.contains("Отчёт по ролям"));
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("Manager"));
        assertTrue(report.contains("Viewer"));
        // админ из initialize() назначен на Admin
        assertTrue(report.contains("Users (active)"));
    }

    @Test
    void generatePermissionMatrixContainsResourcesAndPermissions() {
        RBACSystem system = new RBACSystem();
        system.initialize();

        ReportGenerator rg = new ReportGenerator();
        String report = rg.generatePermissionMatrix(system.getUserManager(), system.getAssignmentManager());

        assertTrue(report.contains("Матрица прав"));
        assertTrue(report.contains("User"));
        assertTrue(report.contains("users"));
        assertTrue(report.contains("documents"));
        assertTrue(report.contains("reports"));
        assertTrue(report.contains("READ"));
    }

    @Test
    void exportToFileWritesReport() throws Exception {
        ReportGenerator rg = new ReportGenerator();
        String report = "hello report";

        Path tmp = Files.createTempFile("rbac-report-", ".txt");
        tmp.toFile().deleteOnExit();

        rg.exportToFile(report, tmp.toString());

        String readBack = Files.readString(tmp, StandardCharsets.UTF_8);
        assertEquals(report, readBack);
    }
}

