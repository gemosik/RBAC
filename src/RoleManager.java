import java.util.*;

public class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();

    private Collection<RoleAssignment> assignments = List.of();

    public void setAssignments(Collection<RoleAssignment> assignments) {
        this.assignments = Objects.requireNonNull(assignments);
    }

    @Override
    public void add(Role role) {
        Objects.requireNonNull(role, "role не может быть null");

        String id = role.getId();
        String name = role.getName();

        if (rolesByName.containsKey(name)) {
            throw new IllegalArgumentException("Роль с таким именем уже существует: " + name);
        }

        rolesById.put(id, role);
        rolesByName.put(name, role);
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) {
            return false;
        }

        if (isRoleAssigned(role)) {
            throw new IllegalStateException("Невозможно удалить роль, так как она назначена пользователям");
        }

        Role removedById = rolesById.remove(role.getId());
        rolesByName.remove(role.getName());
        return removedById != null;
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        Objects.requireNonNull(filter, "filter не может быть null");

        List<Role> result = new ArrayList<>();
        for (Role role : rolesById.values()) {
            if (filter.test(role)) {
                result.add(role);
            }
        }
        return result;
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        Objects.requireNonNull(filter, "filter не может быть null");
        Objects.requireNonNull(sorter, "sorter не может быть null");

        List<Role> filtered = findByFilter(filter);
        filtered.sort(sorter);
        return filtered;
    }

    public boolean exists(String name) {
        if (name == null) {
            return false;
        }
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Objects.requireNonNull(roleName, "roleName не может быть null");
        Objects.requireNonNull(permission, "permission не может быть null");

        Role role = rolesByName.get(roleName);
        if (role == null) {
            throw new NoSuchElementException("Роль с именем " + roleName + " не найдена");
        }

        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Objects.requireNonNull(roleName, "roleName не может быть null");
        Objects.requireNonNull(permission, "permission не может быть null");

        Role role = rolesByName.get(roleName);
        if (role == null) {
            throw new NoSuchElementException("Роль с именем " + roleName + " не найдена");
        }

        role.removePermission(permission);
    }

    public void updateRole(String currentName, String newName, String newDescription) {
        ValidationUtils.requireNonEmpty(currentName, "currentName");
        ValidationUtils.requireNonEmpty(newName, "newName");
        ValidationUtils.requireNonEmpty(newDescription, "newDescription");

        String normalizedCurrentName = ValidationUtils.normalizeString(currentName);
        String normalizedNewName = ValidationUtils.normalizeString(newName);
        String normalizedNewDescription = ValidationUtils.normalizeString(newDescription);

        Role existing = rolesByName.get(normalizedCurrentName);
        if (existing == null) {
            throw new NoSuchElementException("Роль с именем " + normalizedCurrentName + " не найдена");
        }

        if (!existing.getName().equals(normalizedNewName) && rolesByName.containsKey(normalizedNewName)) {
            throw new IllegalArgumentException("Роль с таким именем уже существует: " + normalizedNewName);
        }

        rolesByName.remove(existing.getName());
        existing.update(normalizedNewName, normalizedNewDescription);
        rolesByName.put(existing.getName(), existing);
        rolesById.put(existing.getId(), existing);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        Objects.requireNonNull(permissionName, "permissionName не может быть null");
        Objects.requireNonNull(resource, "resource не может быть null");

        List<Role> result = new ArrayList<>();
        for (Role role : rolesById.values()) {
            if (role.hasPermission(permissionName, resource)) {
                result.add(role);
            }
        }
        return result;
    }

    private boolean isRoleAssigned(Role role) {
        if (assignments == null || assignments.isEmpty()) {
            return false;
        }

        for (RoleAssignment assignment : assignments) {
            if (assignment.role().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
