public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void initialize() {
        Permission readUsers = new Permission("READ", "users", "Просмотр пользователей");
        Permission writeUsers = new Permission("WRITE", "users", "Изменение пользователей");
        Permission deleteUsers = new Permission("DELETE", "users", "Удаление пользователей");
        Permission readDocuments = new Permission("READ", "documents", "Просмотр документов");
        Permission writeDocuments = new Permission("WRITE", "documents", "Изменение документов");
        Permission deleteDocuments = new Permission("DELETE", "documents", "Удаление документов");
        Permission readReports = new Permission("READ", "reports", "Просмотр отчётов");
        Permission writeReports = new Permission("WRITE", "reports", "Создание и изменение отчётов");

        Role adminRole = new Role("Admin", "Полный доступ ко всем ресурсам системы");
        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);
        adminRole.addPermission(readDocuments);
        adminRole.addPermission(writeDocuments);
        adminRole.addPermission(deleteDocuments);
        adminRole.addPermission(readReports);
        adminRole.addPermission(writeReports);

        Role managerRole = new Role("Manager", "Управление ресурсами без удаления");
        managerRole.addPermission(readUsers);
        managerRole.addPermission(writeUsers);
        managerRole.addPermission(readDocuments);
        managerRole.addPermission(writeDocuments);
        managerRole.addPermission(readReports);
        managerRole.addPermission(writeReports);

        Role viewerRole = new Role("Viewer", "Только просмотр ресурсов");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readDocuments);
        viewerRole.addPermission(readReports);

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        User adminUser = User.create("admin", "System Administrator", "admin@system.local");
        userManager.add(adminUser);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "Начальная инициализация системы");
        PermanentAssignment adminAssignment = new PermanentAssignment(adminUser, adminRole, metadata);
        assignmentManager.add(adminAssignment);
    }

    public String generateStatistics() {
        int usersCount = userManager.count();
        int rolesCount = roleManager.count();
        int assignmentsCount = assignmentManager.count();
        int activeAssignments = assignmentManager.getActiveAssignments().size();

        return String.format(
                "=== Статистика системы RBAC ===%n" +
                "Пользователей:     %d%n" +
                "Ролей:             %d%n" +
                "Всего назначений:  %d%n" +
                "Активных назначений: %d%n" +
                "Текущий пользователь (админ): %s%n" +
                "=================================",
                usersCount,
                rolesCount,
                assignmentsCount,
                activeAssignments,
                currentUser != null ? currentUser : "—"
        );
    }
}
