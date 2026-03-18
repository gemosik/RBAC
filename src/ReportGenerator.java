import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ReportGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        Objects.requireNonNull(userManager, "userManager не может быть null");
        Objects.requireNonNull(assignmentManager, "assignmentManager не может быть null");

        List<User> users = new ArrayList<>(userManager.findAll());
        users.sort(Comparator.comparing(User::username));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Отчёт по пользователям (roles) ===%n"));
        sb.append(String.format("Generated at: %s%n", LocalDateTime.now().format(TS)));
        sb.append(String.format("Users: %d%n", users.size()));
        sb.append(System.lineSeparator());

        if (users.isEmpty()) {
            sb.append(String.format("(пользователи отсутствуют)%n"));
            return sb.toString();
        }

        for (User user : users) {
            List<RoleAssignment> active = assignmentManager.findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .sorted(Comparator.comparing(a -> a.role().getName()))
                    .toList();

            List<String> roleNames = active.stream()
                    .map(a -> a.role().getName())
                    .distinct()
                    .sorted()
                    .toList();

            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            Set<String> resources = permissions.stream().map(Permission::resource).collect(Collectors.toCollection(TreeSet::new));

            sb.append(String.format("- %s%n", user.format()));
            sb.append(String.format("  Roles (%d): %s%n", roleNames.size(), roleNames.isEmpty() ? "—" : String.join(", ", roleNames)));
            sb.append(String.format("  Permissions: %d, resources: %d%n", permissions.size(), resources.size()));
            if (!resources.isEmpty()) {
                sb.append(String.format("  Resources: %s%n", String.join(", ", resources)));
            }
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        Objects.requireNonNull(roleManager, "roleManager не может быть null");
        Objects.requireNonNull(assignmentManager, "assignmentManager не может быть null");

        List<Role> roles = new ArrayList<>(roleManager.findAll());
        roles.sort(Comparator.comparing(Role::getName));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Отчёт по ролям (users count) ===%n"));
        sb.append(String.format("Generated at: %s%n", LocalDateTime.now().format(TS)));
        sb.append(String.format("Roles: %d%n", roles.size()));
        sb.append(System.lineSeparator());

        if (roles.isEmpty()) {
            sb.append(String.format("(роли отсутствуют)%n"));
            return sb.toString();
        }

        List<String[]> rows = new ArrayList<>();

        int i = 1;
        for (Role role : roles) {
            int userCount = countDistinctActiveUsersForRole(assignmentManager, role);
            rows.add(new String[]{
                    String.valueOf(i++),
                    role.getName(),
                    String.valueOf(userCount),
                    String.valueOf(role.getPermissions().size())
            });
        }

        sb.append(FormatUtils.formatTable(
                new String[]{"#", "Role", "Users (active)", "Permissions"},
                rows
        ));
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        Objects.requireNonNull(userManager, "userManager не может быть null");
        Objects.requireNonNull(assignmentManager, "assignmentManager не может быть null");

        List<User> users = new ArrayList<>(userManager.findAll());
        users.sort(Comparator.comparing(User::username));

        Set<String> resources = new TreeSet<>();
        Map<String, Map<String, Set<String>>> matrix = new HashMap<>();

        for (User u : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(u);
            Map<String, Set<String>> perResource = new HashMap<>();
            for (Permission p : perms) {
                resources.add(p.resource());
                perResource.computeIfAbsent(p.resource(), k -> new TreeSet<>()).add(p.name());
            }
            matrix.put(u.username(), perResource);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Матрица прав (users × resources) ===%n"));
        sb.append(String.format("Generated at: %s%n", LocalDateTime.now().format(TS)));
        sb.append(String.format("Users: %d, Resources: %d%n", users.size(), resources.size()));
        sb.append(System.lineSeparator());

        if (users.isEmpty()) {
            sb.append(String.format("(пользователи отсутствуют)%n"));
            return sb.toString();
        }

        if (resources.isEmpty()) {
            sb.append(String.format("(ресурсы/права отсутствуют)%n"));
            return sb.toString();
        }

        String[] headers = new String[1 + resources.size()];
        headers[0] = "User";
        int hi = 1;
        for (String res : resources) {
            headers[hi++] = res;
        }

        List<String[]> rows = new ArrayList<>();

        for (User u : users) {
            List<String> row = new ArrayList<>();
            row.add(u.username());

            Map<String, Set<String>> perRes = matrix.getOrDefault(u.username(), Map.of());
            for (String res : resources) {
                Set<String> names = perRes.getOrDefault(res, Set.of());
                row.add(names.isEmpty() ? "—" : String.join(",", names));
            }
            rows.add(row.toArray(new String[0]));
        }

        sb.append(FormatUtils.formatTable(headers, rows));
        sb.append(System.lineSeparator());
        sb.append(String.format("Legend: cell содержит список permission.name (например, READ,WRITE).%n"));
        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        Objects.requireNonNull(report, "report не может быть null");
        Objects.requireNonNull(filename, "filename не может быть null");

        String trimmed = filename.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("filename не может быть пустым");
        }

        try {
            Files.writeString(Path.of(trimmed), report, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить отчёт в файл: " + e.getMessage(), e);
        }
    }

    private static int countDistinctActiveUsersForRole(AssignmentManager assignmentManager, Role role) {
        Set<String> usernames = new HashSet<>();
        for (RoleAssignment a : assignmentManager.findByRole(role)) {
            if (!a.isActive()) {
                continue;
            }
            usernames.add(a.user().username());
        }
        return usernames.size();
    }
}
