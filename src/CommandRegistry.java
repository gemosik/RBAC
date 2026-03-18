import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

public final class CommandRegistry {

    private CommandRegistry() {}

    public static void registerAll(CommandParser parser) {
        Objects.requireNonNull(parser, "parser не может быть null");

        parser.registerCommand("help", "Справка по командам", (scanner, system) -> parser.printHelp());
        parser.registerCommand("stats", "Статистика системы", CommandRegistry::stats);
        parser.registerCommand("clear", "Очистить экран", (scanner, system) -> clear());
        parser.registerCommand("exit", "Выход из программы", CommandRegistry::exit);

        parser.registerCommand("user-list", "Список пользователей (опц. фильтры: username=<sub> email=<sub> domain=<d> name=<sub>)", CommandRegistry::userList);
        parser.registerCommand("user-create", "Создать пользователя", CommandRegistry::userCreate);
        parser.registerCommand("user-view", "Просмотр пользователя (роли и права)", CommandRegistry::userView);
        parser.registerCommand("user-update", "Обновить данные пользователя", CommandRegistry::userUpdate);
        parser.registerCommand("user-delete", "Удалить пользователя (с подтверждением)", CommandRegistry::userDelete);
        parser.registerCommand("user-search", "Поиск пользователей по фильтрам (меню)", CommandRegistry::userSearch);

        parser.registerCommand("role-list", "Список ролей", CommandRegistry::roleList);
        parser.registerCommand("role-create", "Создать роль (+ добавление прав)", CommandRegistry::roleCreate);
        parser.registerCommand("role-view", "Просмотр роли (Role.format())", CommandRegistry::roleView);
        parser.registerCommand("role-update", "Обновить роль (название/описание)", CommandRegistry::roleUpdate);
        parser.registerCommand("role-delete", "Удалить роль (если не назначена)", CommandRegistry::roleDelete);
        parser.registerCommand("role-add-permission", "Добавить право к роли", CommandRegistry::roleAddPermission);
        parser.registerCommand("role-remove-permission", "Удалить право из роли", CommandRegistry::roleRemovePermission);
        parser.registerCommand("role-search", "Поиск ролей (меню)", CommandRegistry::roleSearch);

        parser.registerCommand("assign-role", "Назначить роль пользователю", CommandRegistry::assignRole);
        parser.registerCommand("revoke-role", "Отозвать роль у пользователя", CommandRegistry::revokeRole);
        parser.registerCommand("assignment-list", "Все назначения (таблица)", CommandRegistry::assignmentList);
        parser.registerCommand("assignment-list-user", "Назначения пользователя", CommandRegistry::assignmentListUser);
        parser.registerCommand("assignment-list-role", "Пользователи с ролью", CommandRegistry::assignmentListRole);
        parser.registerCommand("assignment-active", "Только активные назначения", CommandRegistry::assignmentActive);
        parser.registerCommand("assignment-expired", "Истёкшие временные назначения", CommandRegistry::assignmentExpired);
        parser.registerCommand("assignment-extend", "Продлить временное назначение", CommandRegistry::assignmentExtend);
        parser.registerCommand("assignment-search", "Поиск назначений (меню)", CommandRegistry::assignmentSearch);

        parser.registerCommand("permissions-user", "Права пользователя (по ресурсам)", CommandRegistry::permissionsUser);
        parser.registerCommand("permissions-check", "Проверка права у пользователя", CommandRegistry::permissionsCheck);
        parser.registerCommand("audit-log", "Просмотр audit log (и сохранение в файл)", CommandRegistry::auditLog);

        parser.registerCommand("report-users", "Отчёт по пользователям с их ролями (вывести/сохранить)", CommandRegistry::reportUsers);
        parser.registerCommand("report-roles", "Отчёт по ролям с количеством пользователей (вывести/сохранить)", CommandRegistry::reportRoles);
        parser.registerCommand("report-matrix", "Матрица прав (users × resources) (вывести/сохранить)", CommandRegistry::reportMatrix);
    }



    private static void userList(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();

        String line = ConsoleUtils.promptString(
                scanner,
                "Фильтры (key=value через пробел, пусто = все). Доступно: username, email, domain, name",
                false
        );

        List<User> users;
        if (line.isEmpty()) {
            users = um.findAll();
            users.sort(UserSorters.byUsername());
        } else {
            UserFilter filter = parseUserListFilters(line);
            users = um.findAll(filter, UserSorters.byUsername());
        }

        printUsersTable(users);
    }

    private static void userCreate(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();

        System.out.println(ConsoleUtils.formatHeader("Создание пользователя"));
        String username = ConsoleUtils.promptString(scanner, "username", true);
        String fullName = ConsoleUtils.promptString(scanner, "fullName", true);
        String email = ConsoleUtils.promptString(scanner, "email", true);

        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        try {
            User user = User.create(username, fullName, email);
            um.add(user);
            System.out.println("Пользователь создан: " + user.format());
            system.getAuditLog().log("USER_CREATE", performer, username, "fullName=" + fullName + ", email=" + email);
        } catch (Exception e) {
            System.out.println("Ошибка создания пользователя: " + e.getMessage());
            system.getAuditLog().log("USER_CREATE_FAILED", performer, username, e.getMessage());
        }
    }

    private static void userView(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Просмотр пользователя"));
        String username = ConsoleUtils.promptString(scanner, "username", true);
        Optional<User> opt = um.findByUsername(username);
        if (opt.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Пользователь не найден: " + username));
            return;
        }

        User user = opt.get();
        System.out.println(FormatUtils.formatBox(user.format()));

        List<RoleAssignment> assignments = am.findByUser(user);
        List<RoleAssignment> active = assignments.stream().filter(RoleAssignment::isActive).toList();

        System.out.println();
        System.out.println(ConsoleUtils.formatHeader("Назначенные роли (активные): " + active.size()));
        if (active.isEmpty()) {
            System.out.println(FormatUtils.formatBox("(нет)"));
        } else {
            List<String[]> rows = new ArrayList<>();
            for (RoleAssignment a : active.stream().sorted(AssignmentSorters.byRoleName()).toList()) {
                rows.add(new String[]{a.role().getName(), a.assignmentType(), a.assignmentId()});
            }
            System.out.println(FormatUtils.formatTable(
                    new String[]{"Role", "Type", "Assignment ID"},
                    rows
            ));
        }

        Set<Permission> perms = am.getUserPermissions(user);
        System.out.println();
        System.out.println(ConsoleUtils.formatHeader("Права (" + perms.size() + ")"));
        if (perms.isEmpty()) {
            System.out.println(FormatUtils.formatBox("(нет)"));
        } else {
            Map<String, List<Permission>> byResource = groupPermissionsByResource(perms);
            for (String resource : byResource.keySet().stream().sorted().toList()) {
                System.out.println();
                System.out.println(ConsoleUtils.formatHeader(resource));
                List<String[]> rows = new ArrayList<>();
                for (Permission p : byResource.get(resource).stream()
                        .sorted(Comparator.comparing(Permission::name))
                        .toList()) {
                    rows.add(new String[]{p.name(), p.description()});
                }
                System.out.println(FormatUtils.formatTable(new String[]{"Permission", "Description"}, rows));
            }
        }
    }

    private static void userUpdate(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();

        System.out.println(ConsoleUtils.formatHeader("Обновление пользователя"));
        String username = ConsoleUtils.promptString(scanner, "username", true);

        if (um.findByUsername(username).isEmpty()) {
            System.out.println("Пользователь не найден: " + username);
            return;
        }

        String fullName = ConsoleUtils.promptString(scanner, "new fullName", true);
        String email = ConsoleUtils.promptString(scanner, "new email", true);

        try {
            um.update(username, fullName, email);
            System.out.println("Пользователь обновлён: " + um.findByUsername(username).orElseThrow().format());
        } catch (Exception e) {
            System.out.println("Ошибка обновления: " + e.getMessage());
        }
    }

    private static void userDelete(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Удаление пользователя"));
        String username = ConsoleUtils.promptString(scanner, "username", true);
        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        Optional<User> opt = um.findByUsername(username);
        if (opt.isEmpty()) {
            System.out.println("Пользователь не найден: " + username);
            system.getAuditLog().log("USER_DELETE_FAILED", performer, username, "user not found");
            return;
        }

        User user = opt.get();
        System.out.println("Удалить пользователя: " + user.format());
        if (!ConsoleUtils.promptYesNo(scanner, "Подтвердите удаление")) {
            System.out.println("Удаление отменено.");
            system.getAuditLog().log("USER_DELETE_CANCELLED", performer, username, "cancelled by user");
            return;
        }

        List<RoleAssignment> toRemove = am.findByUser(user);
        for (RoleAssignment a : toRemove) {
            am.remove(a);
        }

        boolean removed = um.remove(user);
        System.out.println(removed ? "Пользователь удалён." : "Не удалось удалить пользователя.");
        system.getAuditLog().log(
                removed ? "USER_DELETE" : "USER_DELETE_FAILED",
                performer,
                username,
                removed ? "removed with " + toRemove.size() + " assignment(s)" : "remove returned false"
        );
    }

    private static void userSearch(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();

        System.out.println(ConsoleUtils.formatHeader("Поиск пользователей"));
        int choice = ConsoleUtils.promptChoice(scanner, "Выберите фильтр", List.of(
                1,
                2,
                3,
                4
        ));
        UserFilter filter;

        switch (choice) {
            case 1 -> filter = UserFilters.byUsernameContains(ConsoleUtils.promptString(scanner, "substring", true));
            case 2 -> filter = user -> user.email().toLowerCase().contains(ConsoleUtils.promptString(scanner, "substring", true).toLowerCase());
            case 3 -> {
                String domain = ConsoleUtils.promptString(scanner, "domain (например, example.com)", true);
                String normalized = domain.startsWith("@") ? domain.substring(1) : domain;
                filter = UserFilters.byEmailDomain(normalized);
            }
            case 4 -> filter = UserFilters.byFullNameContains(ConsoleUtils.promptString(scanner, "substring", true));
            default -> throw new IllegalStateException("Unexpected value: " + choice);
        }

        List<User> result = um.findAll(filter, UserSorters.byUsername());
        printUsersTable(result);
    }

    private static UserFilter parseUserListFilters(String line) {
        UserFilter filter = user -> true;
        for (String token : line.split("\\s+")) {
            int eq = token.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = token.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = token.substring(eq + 1).trim();
            if (value.isEmpty()) {
                continue;
            }

            switch (key) {
                case "username", "u" -> filter = filter.and(UserFilters.byUsernameContains(value));
                case "email", "e" -> filter = filter.and(user -> user.email().toLowerCase().contains(value.toLowerCase()));
                case "domain", "d" -> filter = filter.and(UserFilters.byEmailDomain(value.startsWith("@") ? value.substring(1) : value));
                case "name", "fullname", "n" -> filter = filter.and(UserFilters.byFullNameContains(value));
                default -> { /* ignore */ }
            }
        }
        return filter;
    }



    private static void roleList(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        List<Role> roles = rm.findAll();
        roles.sort(RoleSorters.byName());

        printRolesTable(roles);
    }

    private static void roleCreate(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();

        System.out.println(ConsoleUtils.formatHeader("Создание роли"));
        String name = ConsoleUtils.promptString(scanner, "role name", true);
        String description = ConsoleUtils.promptString(scanner, "description", true);

        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        try {
            Role role = new Role(name, description);

            while (ConsoleUtils.promptYesNo(scanner, "Добавить право?")) {
                Permission p = promptPermission(scanner);
                role.addPermission(p);
                System.out.println("Добавлено: " + p.format());
            }

            rm.add(role);
            System.out.println("Роль создана: " + role.getName() + " (id=" + role.getId() + ")");
            system.getAuditLog().log("ROLE_CREATE", performer, role.getName(), "permissions=" + role.getPermissions().size());
        } catch (Exception e) {
            System.out.println("Ошибка создания роли: " + e.getMessage());
            system.getAuditLog().log("ROLE_CREATE_FAILED", performer, name, e.getMessage());
        }
    }

    private static void roleView(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        System.out.println(ConsoleUtils.formatHeader("Просмотр роли"));
        String roleName = ConsoleUtils.promptString(scanner, "role name", true);

        Optional<Role> opt = rm.findByName(roleName);
        if (opt.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Роль не найдена: " + roleName));
            return;
        }

        System.out.println(FormatUtils.formatBox(opt.get().format()));
    }

    private static void roleUpdate(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        System.out.println(ConsoleUtils.formatHeader("Обновление роли"));
        String currentName = ConsoleUtils.promptString(scanner, "current role name", true);

        if (rm.findByName(currentName).isEmpty()) {
            System.out.println("Роль не найдена: " + currentName);
            return;
        }

        String newName = ConsoleUtils.promptString(scanner, "new role name", true);
        String newDescription = ConsoleUtils.promptString(scanner, "new description", true);

        try {
            rm.updateRole(currentName, newName, newDescription);
            System.out.println("Роль обновлена.");
        } catch (Exception e) {
            System.out.println("Ошибка обновления роли: " + e.getMessage());
        }
    }

    private static void roleDelete(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Удаление роли"));
        String roleName = ConsoleUtils.promptString(scanner, "role name", true);
        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        Optional<Role> opt = rm.findByName(roleName);
        if (opt.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Роль не найдена: " + roleName));
            system.getAuditLog().log("ROLE_DELETE_FAILED", performer, roleName, "role not found");
            return;
        }

        Role role = opt.get();
        List<RoleAssignment> assigned = am.findByRole(role);
        if (!assigned.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Роль назначена пользователям, удалить нельзя."));
            List<String[]> rows = assigned.stream()
                    .map(a -> a.user().username())
                    .distinct()
                    .sorted()
                    .map(u -> new String[]{u})
                    .toList();
            System.out.println(FormatUtils.formatTable(new String[]{"Users"}, rows));
            system.getAuditLog().log("ROLE_DELETE_FAILED", performer, roleName, "role is assigned (" + assigned.size() + " assignment(s))");
            return;
        }

        System.out.println("Удалить роль: " + role.getName() + " (id=" + role.getId() + ")");
        if (!ConsoleUtils.promptYesNo(scanner, "Подтвердите удаление")) {
            System.out.println("Удаление отменено.");
            system.getAuditLog().log("ROLE_DELETE_CANCELLED", performer, roleName, "cancelled by user");
            return;
        }

        try {
            boolean removed = rm.remove(role);
            System.out.println(removed ? "Роль удалена." : "Не удалось удалить роль.");
            system.getAuditLog().log(removed ? "ROLE_DELETE" : "ROLE_DELETE_FAILED", performer, roleName, removed ? "" : "remove returned false");
        } catch (Exception e) {
            System.out.println("Ошибка удаления роли: " + e.getMessage());
            system.getAuditLog().log("ROLE_DELETE_FAILED", performer, roleName, e.getMessage());
        }
    }

    private static void roleAddPermission(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        System.out.println(ConsoleUtils.formatHeader("Добавление права к роли"));
        String roleName = ConsoleUtils.promptString(scanner, "role name", true);

        if (rm.findByName(roleName).isEmpty()) {
            System.out.println("Роль не найдена: " + roleName);
            return;
        }

        try {
            Permission p = promptPermission(scanner);
            rm.addPermissionToRole(roleName, p);
            System.out.println("Право добавлено: " + p.format());
        } catch (Exception e) {
            System.out.println("Ошибка добавления права: " + e.getMessage());
        }
    }

    private static void roleRemovePermission(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        System.out.println(ConsoleUtils.formatHeader("Удаление права из роли"));
        String roleName = ConsoleUtils.promptString(scanner, "role name", true);
        Optional<Role> opt = rm.findByName(roleName);
        if (opt.isEmpty()) {
            System.out.println("Роль не найдена: " + roleName);
            return;
        }

        Role role = opt.get();
        List<Permission> perms = role.getPermissions().stream()
                .sorted(Comparator.comparing(Permission::resource).thenComparing(Permission::name))
                .toList();

        if (perms.isEmpty()) {
            System.out.println("У роли нет прав.");
            return;
        }

        List<String> permLabels = perms.stream().map(Permission::format).toList();
        String chosenLabel = ConsoleUtils.promptChoice(scanner, "Выберите право для удаления", permLabels);
        int chosenIdx = Math.max(0, permLabels.indexOf(chosenLabel));
        Permission p = perms.get(chosenIdx);

        try {
            rm.removePermissionFromRole(roleName, p);
            System.out.println("Право удалено: " + p.format());
        } catch (Exception e) {
            System.out.println("Ошибка удаления права: " + e.getMessage());
        }
    }

    private static void roleSearch(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();

        System.out.println(ConsoleUtils.formatHeader("Поиск ролей"));
        int choice = ConsoleUtils.promptInt(scanner, "Выберите пункт", 1, 3);
        RoleFilter filter;

        switch (choice) {
            case 1 -> filter = RoleFilters.byNameContains(ConsoleUtils.promptString(scanner, "substring", true));
            case 2 -> {
                String permName = ConsoleUtils.promptString(scanner, "permission name", true);
                String resource = ConsoleUtils.promptString(scanner, "resource", true);
                filter = RoleFilters.hasPermission(permName, resource);
            }
            case 3 -> filter = RoleFilters.hasAtLeastNPermissions(ConsoleUtils.promptInt(scanner, "min permissions", 0, 1_000_000));
            default -> throw new IllegalStateException("Unexpected value: " + choice);
        }

        List<Role> roles = rm.findAll(filter, RoleSorters.byName());
        printRolesTable(roles);
    }



    private static void assignRole(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        RoleManager rm = system.getRoleManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Назначение роли"));
        String username = ConsoleUtils.promptString(scanner, "username", true);
        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println(FormatUtils.formatBox("Пользователь не найден: " + username));
            system.getAuditLog().log("ASSIGN_ROLE_FAILED", performer, username, "user not found");
            return;
        }

        List<Role> roles = rm.findAll();
        roles.sort(RoleSorters.byName());
        if (roles.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Роли отсутствуют."));
            return;
        }

        Role role = ConsoleUtils.promptChoice(scanner, "Выберите роль", roles);

        String type = ConsoleUtils.promptChoice(scanner, "Тип назначения", List.of("Постоянное", "Временное"));

        String reason = ConsoleUtils.promptString(scanner, "reason", true);
        String assignedBy = performer;
        AssignmentMetadata meta = AssignmentMetadata.now(assignedBy, reason);

        try {
            RoleAssignment assignment;
            if (type.equals("Постоянное")) {
                assignment = new PermanentAssignment(user, role, meta);
            } else {
                String expiresAt = ConsoleUtils.promptString(scanner, "expiresAt (YYYY-MM-DD или YYYY-MM-DD HH:MM)", true);
                assignment = new TemporaryAssignment(user, role, meta, expiresAt);
            }

            am.add(assignment);
            System.out.println("Назначение создано: id=" + assignment.assignmentId());
            system.getAuditLog().log(
                    "ASSIGN_ROLE",
                    performer,
                    username,
                    "role=" + role.getName() + ", type=" + assignment.assignmentType() + ", id=" + assignment.assignmentId() + ", reason=" + reason
            );
        } catch (Exception e) {
            System.out.println("Ошибка назначения роли: " + e.getMessage());
            system.getAuditLog().log("ASSIGN_ROLE_FAILED", performer, username, "role=" + role.getName() + ", error=" + e.getMessage());
        }
    }

    private static void revokeRole(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Отзыв роли"));
        String username = ConsoleUtils.promptString(scanner, "username", true);
        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println(FormatUtils.formatBox("Пользователь не найден: " + username));
            system.getAuditLog().log("REVOKE_ROLE_FAILED", performer, username, "user not found");
            return;
        }

        List<RoleAssignment> active = am.findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .sorted(AssignmentSorters.byRoleName())
                .toList();

        if (active.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Активных назначений нет."));
            return;
        }

        RoleAssignment chosen = ConsoleUtils.promptChoice(scanner, "Выберите назначение для отзыва", active);

        try {
            if (chosen instanceof PermanentAssignment pa) {
                pa.revoke();
                System.out.println("Постоянное назначение отозвано (помечено неактивным).");
                system.getAuditLog().log(
                        "REVOKE_ROLE",
                        performer,
                        username,
                        "role=" + chosen.role().getName() + ", type=" + chosen.assignmentType() + ", id=" + chosen.assignmentId() + ", method=mark-inactive"
                );
            } else {
                am.revokeAssignment(chosen.assignmentId());
                System.out.println("Назначение удалено.");
                system.getAuditLog().log(
                        "REVOKE_ROLE",
                        performer,
                        username,
                        "role=" + chosen.role().getName() + ", type=" + chosen.assignmentType() + ", id=" + chosen.assignmentId() + ", method=remove"
                );
            }
        } catch (Exception e) {
            System.out.println("Ошибка отзыва: " + e.getMessage());
            system.getAuditLog().log(
                    "REVOKE_ROLE_FAILED",
                    performer,
                    username,
                    "role=" + chosen.role().getName() + ", id=" + chosen.assignmentId() + ", error=" + e.getMessage()
            );
        }
    }

    private static void auditLog(Scanner scanner, RBACSystem system) {
        AuditLog log = system.getAuditLog();
        log.printLog();

        if (ConsoleUtils.promptYesNo(scanner, "Сохранить лог в файл?")) {
            String filename = ConsoleUtils.promptString(scanner, "filename", true);
            log.saveToFile(filename);
        }
    }

    private static void reportUsers(Scanner scanner, RBACSystem system) {
        ReportGenerator rg = new ReportGenerator();
        String report = rg.generateUserReport(system.getUserManager(), system.getAssignmentManager());
        System.out.println(report);
        exportReport(scanner, rg, report);
    }

    private static void reportRoles(Scanner scanner, RBACSystem system) {
        ReportGenerator rg = new ReportGenerator();
        String report = rg.generateRoleReport(system.getRoleManager(), system.getAssignmentManager());
        System.out.println(report);
        exportReport(scanner, rg, report);
    }

    private static void reportMatrix(Scanner scanner, RBACSystem system) {
        ReportGenerator rg = new ReportGenerator();
        String report = rg.generatePermissionMatrix(system.getUserManager(), system.getAssignmentManager());
        System.out.println(report);
        exportReport(scanner, rg, report);
    }

    private static void exportReport(Scanner scanner, ReportGenerator rg, String report) {
        if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
            String filename = ConsoleUtils.promptString(scanner, "filename", true);
            rg.exportToFile(report, filename);
            System.out.println("Отчёт сохранён в файл: " + filename);
        }
    }

    private static void assignmentList(Scanner scanner, RBACSystem system) {
        List<RoleAssignment> list = system.getAssignmentManager().findAll();
        list.sort(AssignmentSorters.byAssignmentDate().thenComparing(AssignmentSorters.byUsername()).thenComparing(AssignmentSorters.byRoleName()));
        printAssignmentsTable(list);
    }

    private static void assignmentListUser(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Назначения пользователя"));
        String username = ConsoleUtils.promptString(scanner, "username", true);
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println(FormatUtils.formatBox("Пользователь не найден: " + username));
            return;
        }

        List<RoleAssignment> list = am.findByUser(user);
        list.sort(AssignmentSorters.byAssignmentDate());
        printAssignmentsTable(list);
    }

    private static void assignmentListRole(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Пользователи с ролью"));
        String roleName = ConsoleUtils.promptString(scanner, "role name", true);
        Role role = rm.findByName(roleName).orElse(null);
        if (role == null) {
            System.out.println(FormatUtils.formatBox("Роль не найдена: " + roleName));
            return;
        }

        List<RoleAssignment> list = am.findByRole(role);
        list.sort(AssignmentSorters.byUsername());

        if (list.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Назначений для роли нет."));
            return;
        }

        System.out.println(ConsoleUtils.formatHeader("Пользователи с ролью " + role.getName()));
        List<String[]> rows = list.stream()
                .map(a -> a.user().username())
                .distinct()
                .sorted()
                .map(u -> new String[]{u})
                .toList();
        System.out.println(FormatUtils.formatTable(new String[]{"Username"}, rows));
    }

    private static void assignmentActive(Scanner scanner, RBACSystem system) {
        List<RoleAssignment> list = system.getAssignmentManager().getActiveAssignments();
        list.sort(AssignmentSorters.byAssignmentDate());
        printAssignmentsTable(list);
    }

    private static void assignmentExpired(Scanner scanner, RBACSystem system) {
        List<RoleAssignment> list = system.getAssignmentManager().getExpiredAssignments();
        list.sort(AssignmentSorters.byAssignmentDate());
        printAssignmentsTable(list);
    }

    private static void assignmentExtend(Scanner scanner, RBACSystem system) {
        AssignmentManager am = system.getAssignmentManager();
        System.out.println(ConsoleUtils.formatHeader("Продление временного назначения"));
        String assignmentId = ConsoleUtils.promptString(scanner, "assignment id", true);
        String newExp = ConsoleUtils.promptString(scanner, "new expiration (YYYY-MM-DD или YYYY-MM-DD HH:MM)", true);

        try {
            am.extendTemporaryAssignment(assignmentId, newExp);
            System.out.println("Временное назначение продлено.");
        } catch (Exception e) {
            System.out.println("Ошибка продления: " + e.getMessage());
        }
    }

    private static void assignmentSearch(Scanner scanner, RBACSystem system) {
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Поиск назначений"));
        int choice = ConsoleUtils.promptInt(scanner, "Выберите пункт (1-6)", 1, 6);
        AssignmentFilter filter;

        switch (choice) {
            case 1 -> filter = AssignmentFilters.byUsername(ConsoleUtils.promptString(scanner, "username", true));
            case 2 -> filter = AssignmentFilters.byRoleName(ConsoleUtils.promptString(scanner, "role name", true));
            case 3 -> filter = AssignmentFilters.byType(ConsoleUtils.promptString(scanner, "type", true));
            case 4 -> {
                int st = ConsoleUtils.promptInt(scanner, "Статус: 1) ACTIVE  2) INACTIVE", 1, 2);
                filter = st == 1 ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
            }
            case 5 -> filter = AssignmentFilters.assignedAfter(ConsoleUtils.promptString(scanner, "dateTime", true));
            case 6 -> filter = AssignmentFilters.expiringBefore(ConsoleUtils.promptString(scanner, "dateTime", true));
            default -> throw new IllegalStateException("Unexpected value: " + choice);
        }

        List<RoleAssignment> list = am.findByFilter(filter);
        list.sort(AssignmentSorters.byAssignmentDate());
        printAssignmentsTable(list);
    }



    private static void permissionsUser(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Права пользователя"));
        String username = ConsoleUtils.promptString(scanner, "username", true);
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println(FormatUtils.formatBox("Пользователь не найден: " + username));
            return;
        }

        Set<Permission> perms = am.getUserPermissions(user);
        if (perms.isEmpty()) {
            System.out.println(FormatUtils.formatBox("У пользователя нет прав."));
            return;
        }

        Map<String, List<Permission>> byResource = groupPermissionsByResource(perms);
        for (String resource : byResource.keySet().stream().sorted().toList()) {
            System.out.println();
            System.out.println(ConsoleUtils.formatHeader(resource));
            List<String[]> rows = byResource.get(resource).stream()
                    .sorted(Comparator.comparing(Permission::name))
                    .map(p -> new String[]{p.name(), p.description()})
                    .toList();
            System.out.println(FormatUtils.formatTable(new String[]{"Permission", "Description"}, rows));
        }
    }

    private static void permissionsCheck(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        AssignmentManager am = system.getAssignmentManager();

        System.out.println(ConsoleUtils.formatHeader("Проверка права"));
        String username = ConsoleUtils.promptString(scanner, "username", true);
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println(FormatUtils.formatBox("Пользователь не найден: " + username));
            return;
        }

        String permName = ConsoleUtils.promptString(scanner, "permission name", true);
        String resource = ConsoleUtils.promptString(scanner, "resource", true);

        boolean has = am.userHasPermission(user, permName, resource);
        if (!has) {
            System.out.println(FormatUtils.formatBox("НЕТ: у пользователя нет такого права."));
            return;
        }

        List<String> roles = new ArrayList<>();
        for (RoleAssignment a : am.findByUser(user)) {
            if (!a.isActive()) continue;
            if (a.role().hasPermission(permName, resource)) {
                roles.add(a.role().getName());
            }
        }
        roles = roles.stream().distinct().sorted().toList();

        System.out.println(FormatUtils.formatBox("ДА: право есть."));
        System.out.println(FormatUtils.formatTable(
                new String[]{"Roles granting permission"},
                roles.stream().map(r -> new String[]{r}).toList()
        ));
    }



    private static void stats(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        RoleManager rm = system.getRoleManager();
        AssignmentManager am = system.getAssignmentManager();

        int users = um.count();
        int roles = rm.count();
        int totalAssignments = am.count();
        int activeAssignments = am.getActiveAssignments().size();
        int expiredAssignments = am.getExpiredAssignments().size();

        double avgRolesPerUser = users == 0 ? 0.0 : (double) activeAssignments / users;

        Map<String, Integer> rolePopularity = new HashMap<>();
        for (RoleAssignment a : am.getActiveAssignments()) {
            rolePopularity.merge(a.role().getName(), 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> top = rolePopularity.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .toList();

        System.out.println(system.generateStatistics());
        System.out.println();
        System.out.println(ConsoleUtils.formatHeader("Дополнительно"));
        System.out.println(FormatUtils.formatTable(
                new String[]{"Metric", "Value"},
                List.of(
                        new String[]{"Истёкших временных назначений", String.valueOf(expiredAssignments)},
                        new String[]{"Среднее ролей на пользователя (активные)", String.format(Locale.ROOT, "%.2f", avgRolesPerUser)}
                )
        ));

        System.out.println();
        System.out.println(ConsoleUtils.formatHeader("Топ-3 самых популярных ролей (активные назначения)"));
        if (top.isEmpty()) {
            System.out.println(FormatUtils.formatBox("(нет)"));
        } else {
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < top.size(); i++) {
                var e = top.get(i);
                rows.add(new String[]{String.valueOf(i + 1), e.getKey(), String.valueOf(e.getValue())});
            }
            System.out.println(FormatUtils.formatTable(new String[]{"#", "Role", "Active assignments"}, rows));
        }
    }

    private static void clear() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }

    private static void exit(Scanner scanner, RBACSystem system) {
        if (!ConsoleUtils.promptYesNo(scanner, "Подтвердите выход")) {
            System.out.println("Выход отменён.");
            return;
        }
        System.out.println("Завершение программы.");
        System.exit(0);
    }

    private static Permission promptPermission(Scanner scanner) {
        String name = ConsoleUtils.promptString(scanner, "permission name", true);
        String resource = ConsoleUtils.promptString(scanner, "resource", true);
        String description = ConsoleUtils.promptString(scanner, "description", true);
        return new Permission(name, resource, description);
    }

    private static void printUsersTable(List<User> users) {
        if (users == null || users.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Пользователи не найдены."));
            return;
        }

        List<String[]> rows = new ArrayList<>();
        int i = 1;
        for (User u : users) {
            rows.add(new String[]{String.valueOf(i++), u.username(), u.fullName(), u.email()});
        }
        System.out.println(FormatUtils.formatTable(new String[]{"#", "Username", "Full Name", "Email"}, rows));
    }

    private static void printRolesTable(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Роли не найдены."));
            return;
        }

        List<String[]> rows = new ArrayList<>();
        int i = 1;
        for (Role r : roles) {
            rows.add(new String[]{String.valueOf(i++), r.getName(), String.valueOf(r.getPermissions().size()), r.getId()});
        }
        System.out.println(FormatUtils.formatTable(new String[]{"#", "Name", "Permissions", "ID"}, rows));
    }

    private static void printAssignmentsTable(List<RoleAssignment> list) {
        if (list == null || list.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Назначения не найдены."));
            return;
        }

        List<String[]> rows = new ArrayList<>();
        int i = 1;
        for (RoleAssignment a : list) {
            String status;
            if (a instanceof TemporaryAssignment t) {
                status = t.isExpired() ? "EXPIRED" : "ACTIVE";
            } else {
                status = a.isActive() ? "ACTIVE" : "INACTIVE";
            }
            rows.add(new String[]{
                    String.valueOf(i++),
                    a.user().username(),
                    a.role().getName(),
                    a.assignmentType(),
                    status,
                    a.metadata().assignedAt(),
                    a.assignmentId()
            });
        }

        System.out.println(FormatUtils.formatTable(
                new String[]{"#", "Username", "Role", "Type", "Status", "Assigned at", "ID"},
                rows
        ));
    }

    private static Map<String, List<Permission>> groupPermissionsByResource(Set<Permission> perms) {
        Map<String, List<Permission>> byResource = new HashMap<>();
        for (Permission p : perms) {
            byResource.computeIfAbsent(p.resource(), k -> new ArrayList<>()).add(p);
        }
        return byResource;
    }
}
