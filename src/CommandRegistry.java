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
    }



    private static void userList(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();

        System.out.print("Фильтры (key=value через пробел, пусто = все). Доступно: username, email, domain, name: ");
        String line = scanner.nextLine().trim();

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

        String username = promptNonBlank(scanner, "username");
        String fullName = promptNonBlank(scanner, "fullName");
        String email = promptNonBlank(scanner, "email");

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

        String username = promptNonBlank(scanner, "username");
        Optional<User> opt = um.findByUsername(username);
        if (opt.isEmpty()) {
            System.out.println("Пользователь не найден: " + username);
            return;
        }

        User user = opt.get();
        System.out.println(user.format());

        List<RoleAssignment> assignments = am.findByUser(user);
        List<RoleAssignment> active = assignments.stream().filter(RoleAssignment::isActive).toList();

        System.out.println();
        System.out.println("Назначенные роли (активные): " + active.size());
        if (active.isEmpty()) {
            System.out.println("- (нет)");
        } else {
            for (RoleAssignment a : active) {
                System.out.println("- " + a.role().getName() + " [" + a.assignmentType() + "] (id=" + a.assignmentId() + ")");
            }
        }

        Set<Permission> perms = am.getUserPermissions(user);
        System.out.println();
        System.out.println("Права (" + perms.size() + "):");
        if (perms.isEmpty()) {
            System.out.println("- (нет)");
        } else {
            Map<String, List<Permission>> byResource = groupPermissionsByResource(perms);
            for (String resource : byResource.keySet().stream().sorted().toList()) {
                System.out.println(resource + ":");
                for (Permission p : byResource.get(resource)) {
                    System.out.println("  - " + p.name() + " — " + p.description());
                }
            }
        }
    }

    private static void userUpdate(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();

        String username = promptNonBlank(scanner, "username");

        if (um.findByUsername(username).isEmpty()) {
            System.out.println("Пользователь не найден: " + username);
            return;
        }

        String fullName = promptNonBlank(scanner, "new fullName");
        String email = promptNonBlank(scanner, "new email");

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

        String username = promptNonBlank(scanner, "username");
        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        Optional<User> opt = um.findByUsername(username);
        if (opt.isEmpty()) {
            System.out.println("Пользователь не найден: " + username);
            system.getAuditLog().log("USER_DELETE_FAILED", performer, username, "user not found");
            return;
        }

        User user = opt.get();
        System.out.println("Удалить пользователя: " + user.format());
        if (!confirm(scanner, "Подтвердите удаление (введите \"да\")")) {
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

        System.out.println("Фильтр поиска пользователей:");
        System.out.println("1) По username (содержит)");
        System.out.println("2) По email (содержит)");
        System.out.println("3) По домену email");
        System.out.println("4) По полному имени (содержит)");

        int choice = promptInt(scanner, "Выберите пункт (1-4)", 1, 4);
        UserFilter filter;

        switch (choice) {
            case 1 -> filter = UserFilters.byUsernameContains(promptNonBlank(scanner, "substring"));
            case 2 -> filter = user -> user.email().toLowerCase().contains(promptNonBlank(scanner, "substring").toLowerCase());
            case 3 -> {
                String domain = promptNonBlank(scanner, "domain (например, example.com)");
                String normalized = domain.startsWith("@") ? domain.substring(1) : domain;
                filter = UserFilters.byEmailDomain(normalized);
            }
            case 4 -> filter = UserFilters.byFullNameContains(promptNonBlank(scanner, "substring"));
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

        String name = promptNonBlank(scanner, "role name");
        String description = promptNonBlank(scanner, "description");

        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        try {
            Role role = new Role(name, description);

            while (confirm(scanner, "Добавить право? (введите \"да\" чтобы добавить)")) {
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
        String roleName = promptNonBlank(scanner, "role name");

        Optional<Role> opt = rm.findByName(roleName);
        if (opt.isEmpty()) {
            System.out.println("Роль не найдена: " + roleName);
            return;
        }

        System.out.println(opt.get().format());
    }

    private static void roleUpdate(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        String currentName = promptNonBlank(scanner, "current role name");

        if (rm.findByName(currentName).isEmpty()) {
            System.out.println("Роль не найдена: " + currentName);
            return;
        }

        String newName = promptNonBlank(scanner, "new role name");
        String newDescription = promptNonBlank(scanner, "new description");

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

        String roleName = promptNonBlank(scanner, "role name");
        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        Optional<Role> opt = rm.findByName(roleName);
        if (opt.isEmpty()) {
            System.out.println("Роль не найдена: " + roleName);
            system.getAuditLog().log("ROLE_DELETE_FAILED", performer, roleName, "role not found");
            return;
        }

        Role role = opt.get();
        List<RoleAssignment> assigned = am.findByRole(role);
        if (!assigned.isEmpty()) {
            System.out.println("Роль назначена пользователям, удалить нельзя. Пользователи:");
            assigned.stream()
                    .map(a -> a.user().username())
                    .distinct()
                    .sorted()
                    .forEach(u -> System.out.println("- " + u));
            system.getAuditLog().log("ROLE_DELETE_FAILED", performer, roleName, "role is assigned (" + assigned.size() + " assignment(s))");
            return;
        }

        System.out.println("Удалить роль: " + role.getName() + " (id=" + role.getId() + ")");
        if (!confirm(scanner, "Подтвердите удаление (введите \"да\")")) {
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
        String roleName = promptNonBlank(scanner, "role name");

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
        String roleName = promptNonBlank(scanner, "role name");
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

        System.out.println("Права роли:");
        for (int i = 0; i < perms.size(); i++) {
            System.out.println((i + 1) + ") " + perms.get(i).format());
        }

        int idx = promptInt(scanner, "Номер права для удаления", 1, perms.size());
        Permission p = perms.get(idx - 1);

        try {
            rm.removePermissionFromRole(roleName, p);
            System.out.println("Право удалено: " + p.format());
        } catch (Exception e) {
            System.out.println("Ошибка удаления права: " + e.getMessage());
        }
    }

    private static void roleSearch(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();

        System.out.println("Фильтр поиска ролей:");
        System.out.println("1) По имени (содержит)");
        System.out.println("2) По наличию конкретного права");
        System.out.println("3) По минимальному количеству прав");

        int choice = promptInt(scanner, "Выберите пункт (1-3)", 1, 3);
        RoleFilter filter;

        switch (choice) {
            case 1 -> filter = RoleFilters.byNameContains(promptNonBlank(scanner, "substring"));
            case 2 -> {
                String permName = promptNonBlank(scanner, "permission name");
                String resource = promptNonBlank(scanner, "resource");
                filter = RoleFilters.hasPermission(permName, resource);
            }
            case 3 -> filter = RoleFilters.hasAtLeastNPermissions(promptInt(scanner, "min permissions", 0, 1_000_000));
            default -> throw new IllegalStateException("Unexpected value: " + choice);
        }

        List<Role> roles = rm.findAll(filter, RoleSorters.byName());
        printRolesTable(roles);
    }



    private static void assignRole(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        RoleManager rm = system.getRoleManager();
        AssignmentManager am = system.getAssignmentManager();

        String username = promptNonBlank(scanner, "username");
        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println("Пользователь не найден: " + username);
            system.getAuditLog().log("ASSIGN_ROLE_FAILED", performer, username, "user not found");
            return;
        }

        List<Role> roles = rm.findAll();
        roles.sort(RoleSorters.byName());
        if (roles.isEmpty()) {
            System.out.println("Роли отсутствуют.");
            return;
        }

        System.out.println("Доступные роли:");
        for (int i = 0; i < roles.size(); i++) {
            Role r = roles.get(i);
            System.out.println((i + 1) + ") " + r.getName() + " (" + r.getPermissions().size() + " perms)");
        }

        int roleIdx = promptInt(scanner, "Выберите роль", 1, roles.size());
        Role role = roles.get(roleIdx - 1);

        System.out.println("Тип назначения:");
        System.out.println("1) Постоянное");
        System.out.println("2) Временное");
        int type = promptInt(scanner, "Выберите тип (1-2)", 1, 2);

        String reason = promptNonBlank(scanner, "reason");
        String assignedBy = performer;
        AssignmentMetadata meta = AssignmentMetadata.now(assignedBy, reason);

        try {
            RoleAssignment assignment;
            if (type == 1) {
                assignment = new PermanentAssignment(user, role, meta);
            } else {
                String expiresAt = promptNonBlank(scanner, "expiresAt (YYYY-MM-DD или YYYY-MM-DD HH:MM)");
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

        String username = promptNonBlank(scanner, "username");
        String performer = system.getCurrentUser() != null ? system.getCurrentUser() : "system";
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println("Пользователь не найден: " + username);
            system.getAuditLog().log("REVOKE_ROLE_FAILED", performer, username, "user not found");
            return;
        }

        List<RoleAssignment> active = am.findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .sorted(AssignmentSorters.byRoleName())
                .toList();

        if (active.isEmpty()) {
            System.out.println("Активных назначений нет.");
            return;
        }

        System.out.println("Активные назначения:");
        for (int i = 0; i < active.size(); i++) {
            RoleAssignment a = active.get(i);
            System.out.println((i + 1) + ") " + a.role().getName() + " [" + a.assignmentType() + "] id=" + a.assignmentId());
        }

        int idx = promptInt(scanner, "Выберите назначение для отзыва", 1, active.size());
        RoleAssignment chosen = active.get(idx - 1);

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

        if (confirm(scanner, "Сохранить лог в файл? (введите \"да\")")) {
            String filename = promptNonBlank(scanner, "filename");
            log.saveToFile(filename);
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

        String username = promptNonBlank(scanner, "username");
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println("Пользователь не найден: " + username);
            return;
        }

        List<RoleAssignment> list = am.findByUser(user);
        list.sort(AssignmentSorters.byAssignmentDate());
        printAssignmentsTable(list);
    }

    private static void assignmentListRole(Scanner scanner, RBACSystem system) {
        RoleManager rm = system.getRoleManager();
        AssignmentManager am = system.getAssignmentManager();

        String roleName = promptNonBlank(scanner, "role name");
        Role role = rm.findByName(roleName).orElse(null);
        if (role == null) {
            System.out.println("Роль не найдена: " + roleName);
            return;
        }

        List<RoleAssignment> list = am.findByRole(role);
        list.sort(AssignmentSorters.byUsername());

        if (list.isEmpty()) {
            System.out.println("Назначений для роли нет.");
            return;
        }

        System.out.println("Пользователи с ролью " + role.getName() + ":");
        list.stream()
                .map(a -> a.user().username())
                .distinct()
                .sorted()
                .forEach(u -> System.out.println("- " + u));
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
        String assignmentId = promptNonBlank(scanner, "assignment id");
        String newExp = promptNonBlank(scanner, "new expiration (YYYY-MM-DD или YYYY-MM-DD HH:MM)");

        try {
            am.extendTemporaryAssignment(assignmentId, newExp);
            System.out.println("Временное назначение продлено.");
        } catch (Exception e) {
            System.out.println("Ошибка продления: " + e.getMessage());
        }
    }

    private static void assignmentSearch(Scanner scanner, RBACSystem system) {
        AssignmentManager am = system.getAssignmentManager();

        System.out.println("Фильтр поиска назначений:");
        System.out.println("1) По пользователю (username)");
        System.out.println("2) По роли (role name)");
        System.out.println("3) По типу (PERMANENT/TEMPORARY)");
        System.out.println("4) По статусу (активное/неактивное)");
        System.out.println("5) Назначённые после даты (YYYY-MM-DD HH:MM:SS)");
        System.out.println("6) Истекающие до даты (YYYY-MM-DD или YYYY-MM-DD HH:MM)");

        int choice = promptInt(scanner, "Выберите пункт (1-6)", 1, 6);
        AssignmentFilter filter;

        switch (choice) {
            case 1 -> filter = AssignmentFilters.byUsername(promptNonBlank(scanner, "username"));
            case 2 -> filter = AssignmentFilters.byRoleName(promptNonBlank(scanner, "role name"));
            case 3 -> filter = AssignmentFilters.byType(promptNonBlank(scanner, "type"));
            case 4 -> {
                System.out.println("1) ACTIVE");
                System.out.println("2) INACTIVE");
                int st = promptInt(scanner, "Выберите (1-2)", 1, 2);
                filter = st == 1 ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
            }
            case 5 -> filter = AssignmentFilters.assignedAfter(promptNonBlank(scanner, "dateTime"));
            case 6 -> filter = AssignmentFilters.expiringBefore(promptNonBlank(scanner, "dateTime"));
            default -> throw new IllegalStateException("Unexpected value: " + choice);
        }

        List<RoleAssignment> list = am.findByFilter(filter);
        list.sort(AssignmentSorters.byAssignmentDate());
        printAssignmentsTable(list);
    }



    private static void permissionsUser(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        AssignmentManager am = system.getAssignmentManager();

        String username = promptNonBlank(scanner, "username");
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println("Пользователь не найден: " + username);
            return;
        }

        Set<Permission> perms = am.getUserPermissions(user);
        if (perms.isEmpty()) {
            System.out.println("У пользователя нет прав.");
            return;
        }

        Map<String, List<Permission>> byResource = groupPermissionsByResource(perms);
        for (String resource : byResource.keySet().stream().sorted().toList()) {
            System.out.println(resource + ":");
            byResource.get(resource).stream()
                    .sorted(Comparator.comparing(Permission::name))
                    .forEach(p -> System.out.println("  - " + p.name() + " — " + p.description()));
        }
    }

    private static void permissionsCheck(Scanner scanner, RBACSystem system) {
        UserManager um = system.getUserManager();
        AssignmentManager am = system.getAssignmentManager();

        String username = promptNonBlank(scanner, "username");
        User user = um.findByUsername(username).orElse(null);
        if (user == null) {
            System.out.println("Пользователь не найден: " + username);
            return;
        }

        String permName = promptNonBlank(scanner, "permission name");
        String resource = promptNonBlank(scanner, "resource");

        boolean has = am.userHasPermission(user, permName, resource);
        if (!has) {
            System.out.println("НЕТ: у пользователя нет такого права.");
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

        System.out.println("ДА: право есть.");
        System.out.println("Роли, дающие это право: " + String.join(", ", roles));
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
        System.out.println("Дополнительно:");
        System.out.println("Истёкших временных назначений: " + expiredAssignments);
        System.out.printf(Locale.ROOT, "Среднее количество ролей на пользователя (по активным назначениям): %.2f%n", avgRolesPerUser);

        System.out.println("Топ-3 самых популярных ролей (активные назначения):");
        if (top.isEmpty()) {
            System.out.println("- (нет)");
        } else {
            for (int i = 0; i < top.size(); i++) {
                var e = top.get(i);
                System.out.println((i + 1) + ") " + e.getKey() + " — " + e.getValue());
            }
        }
    }

    private static void clear() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }

    private static void exit(Scanner scanner, RBACSystem system) {
        if (!confirm(scanner, "Подтвердите выход (введите \"да\")")) {
            System.out.println("Выход отменён.");
            return;
        }
        System.out.println("Завершение программы.");
        System.exit(0);
    }



    private static String promptNonBlank(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String s = scanner.nextLine();
            if (s != null && !s.trim().isEmpty()) {
                return s.trim();
            }
            System.out.println("Значение не может быть пустым.");
        }
    }

    private static int promptInt(Scanner scanner, String label, int min, int max) {
        while (true) {
            System.out.print(label + ": ");
            String s = scanner.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) {
                    System.out.println("Введите число от " + min + " до " + max + ".");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Введите корректное число.");
            }
        }
    }

    private static boolean confirm(Scanner scanner, String prompt) {
        System.out.print(prompt + ": ");
        String s = scanner.nextLine();
        return s != null && s.trim().equalsIgnoreCase("да");
    }

    private static Permission promptPermission(Scanner scanner) {
        String name = promptNonBlank(scanner, "permission name");
        String resource = promptNonBlank(scanner, "resource");
        String description = promptNonBlank(scanner, "description");
        return new Permission(name, resource, description);
    }

    private static void printUsersTable(List<User> users) {
        if (users == null || users.isEmpty()) {
            System.out.println("Пользователи не найдены.");
            return;
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"#", "username", "fullName", "email"});
        int i = 1;
        for (User u : users) {
            rows.add(new String[]{String.valueOf(i++), u.username(), u.fullName(), u.email()});
        }
        printTable(rows);
    }

    private static void printRolesTable(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            System.out.println("Роли не найдены.");
            return;
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"#", "name", "permissions", "id"});
        int i = 1;
        for (Role r : roles) {
            rows.add(new String[]{String.valueOf(i++), r.getName(), String.valueOf(r.getPermissions().size()), r.getId()});
        }
        printTable(rows);
    }

    private static void printAssignmentsTable(List<RoleAssignment> list) {
        if (list == null || list.isEmpty()) {
            System.out.println("Назначения не найдены.");
            return;
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"#", "username", "role", "type", "status", "assigned at", "id"});

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

        printTable(rows);
    }

    private static void printTable(List<String[]> rows) {
        int cols = rows.stream().mapToInt(r -> r.length).max().orElse(0);
        int[] widths = new int[cols];

        for (String[] row : rows) {
            for (int c = 0; c < row.length; c++) {
                widths[c] = Math.max(widths[c], safe(row[c]).length());
            }
        }

        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                String cell = c < row.length ? safe(row[c]) : "";
                sb.append(padRight(cell, widths[c]));
                if (c != cols - 1) sb.append(" | ");
            }
            System.out.println(sb);
            if (r == 0) {
                System.out.println("-".repeat(sb.length()));
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    private static Map<String, List<Permission>> groupPermissionsByResource(Set<Permission> perms) {
        Map<String, List<Permission>> byResource = new HashMap<>();
        for (Permission p : perms) {
            byResource.computeIfAbsent(p.resource(), k -> new ArrayList<>()).add(p);
        }
        return byResource;
    }
}
