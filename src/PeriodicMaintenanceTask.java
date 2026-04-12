public final class PeriodicMaintenanceTask implements Runnable {

    private final RBACSystem system;

    public PeriodicMaintenanceTask(RBACSystem system) {
        this.system = system;
    }

    @Override
    public void run() {
        try {
            AssignmentManager am = system.getAssignmentManager();
            int revoked = am.revokeExpiredTemporaryAssignments();

            String details = String.format(
                    "users=%d roles=%d assignments_total=%d assignments_active=%d temp_expired_revoked=%d | %s",
                    system.getUserManager().count(),
                    system.getRoleManager().count(),
                    am.count(),
                    am.getActiveAssignments().size(),
                    revoked,
                    compactStats(system)
            );
            system.getAuditLog().log("MAINTENANCE_TICK", "scheduler", "-", details);
        } catch (Throwable t) {
            try {
                system.getAuditLog().log(
                        "MAINTENANCE_ERROR",
                        "scheduler",
                        "-",
                        t.getClass().getSimpleName() + ": " + t.getMessage()
                );
            } catch (Throwable ignored) {
                // не мешаем остановке
            }
        }
    }

    private static String compactStats(RBACSystem system) {
        return system.generateStatistics()
                .replace(System.lineSeparator(), " | ")
                .trim();
    }
}
