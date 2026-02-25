import java.util.*;

public class Role {
    private final String id;
    private String name;
    private String description;
    private final Set<Permission> permissions;

    public Role(String name, String description) {
        validateNull(name, "name");
        validateNull(description,"description");
        this.id = "role_" + UUID.randomUUID().toString();
        this.name = name.trim();
        this.description = description.trim();
        this.permissions = new HashSet<>();
    }
    public String getId() {
        return id;
    }
    public String getName(){
        return this.name;
    }
    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        return permissions.stream().anyMatch(p ->
                p.matches(permissionName, resource)
        );
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(name).append(" [ID: ").append(id).append("]\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Permissions (").append(permissions.size()).append("):\n");

        if (permissions.isEmpty()) {
            sb.append("- No permissions\n");
        } else {
            for (Permission p : permissions) {
                sb.append("- ").append(p.format()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Role{id='%s', name='%s', permissions=%d}", id, name, permissions.size());
    }

    private static void validateNull(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " не может быть пустым");
        }
    }

    public static void main(){
        Role admin = new Role("admin","all permissions");
        System.out.println(admin.format());
        admin.addPermission(new Permission("steal","users","can steal their data"));
        admin.addPermission(new Permission("blackmail","users","can blackmail users using their data"));
        System.out.println(admin.format());


    }

}
