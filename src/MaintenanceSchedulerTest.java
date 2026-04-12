import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaintenanceSchedulerTest {

    @Test
    void periodicMaintenanceRevokesExpiredTemporaryAndWritesAudit() throws Exception {
        RBACSystem system = new RBACSystem(1);
        try {
            system.initialize();

            User u = User.create("tmpuser", "Tmp User", "tmpuser@example.com");
            system.getUserManager().add(u);
            Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
            TemporaryAssignment temp = new TemporaryAssignment(
                    u,
                    viewer,
                    AssignmentMetadata.now("admin", "scheduler test"),
                    "2000-01-01"
            );
            system.getAssignmentManager().add(temp);

            assertFalse(temp.isRevoked());

            Thread.sleep(2500);
            system.awaitAuditIdle(15_000);

            assertTrue(temp.isRevoked());
            boolean tickLogged = system.getAuditLog().getAll().stream()
                    .anyMatch(e -> "MAINTENANCE_TICK".equals(e.action()));
            assertTrue(tickLogged, "ожидалась запись MAINTENANCE_TICK в audit log");
        } finally {
            system.shutdown();
        }
    }
}
