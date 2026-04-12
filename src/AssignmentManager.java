import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Object lock = new Object();

    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();

    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = Objects.requireNonNull(userManager);
        this.roleManager = Objects.requireNonNull(roleManager);
        synchronized (lock) {
            this.roleManager.setAssignments(assignments.values());
        }
    }

    @Override
    public void add(RoleAssignment assignment) {
        Objects.requireNonNull(assignment, "assignment не может быть null");

        User user = assignment.user();
        Role role = assignment.role();

        synchronized (lock) {
            if (!userManager.exists(user.username())) {
                throw new IllegalStateException("Пользователь не существует: " + user.username());
            }
            if (!roleManager.exists(role.getName())) {
                throw new IllegalStateException("Роль не существует: " + role.getName());
            }

            for (RoleAssignment existing : assignments.values()) {
                if (existing.user().equals(user)
                        && existing.role().equals(role)
                        && existing.isActive()
                        && assignment.isActive()) {
                    throw new IllegalStateException(
                            "Роль " + role.getName() + " уже активно назначена пользователю " + user.username()
                    );
                }
            }

            assignments.put(assignment.assignmentId(), assignment);
        }
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }
        synchronized (lock) {
            return assignments.remove(assignment.assignmentId(), assignment);
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        synchronized (lock) {
            return Optional.ofNullable(assignments.get(id));
        }
    }

    @Override
    public List<RoleAssignment> findAll() {
        synchronized (lock) {
            return new ArrayList<>(assignments.values());
        }
    }

    @Override
    public int count() {
        synchronized (lock) {
            return assignments.size();
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            assignments.clear();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        Objects.requireNonNull(user, "user не может быть null");

        synchronized (lock) {
            List<RoleAssignment> result = new ArrayList<>();
            for (RoleAssignment assignment : assignments.values()) {
                if (assignment.user().equals(user)) {
                    result.add(assignment);
                }
            }
            return result;
        }
    }

    public List<RoleAssignment> findByRole(Role role) {
        Objects.requireNonNull(role, "role не может быть null");

        synchronized (lock) {
            List<RoleAssignment> result = new ArrayList<>();
            for (RoleAssignment assignment : assignments.values()) {
                if (assignment.role().equals(role)) {
                    result.add(assignment);
                }
            }
            return result;
        }
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        Objects.requireNonNull(filter, "filter не может быть null");

        synchronized (lock) {
            List<RoleAssignment> result = new ArrayList<>();
            for (RoleAssignment assignment : assignments.values()) {
                if (filter.test(assignment)) {
                    result.add(assignment);
                }
            }
            return result;
        }
    }

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        Objects.requireNonNull(filter, "filter не может быть null");
        List<RoleAssignment> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(assignments.values());
        }
        return snapshot.parallelStream()
                .filter(filter::test)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        Objects.requireNonNull(filter, "filter не может быть null");
        Objects.requireNonNull(sorter, "sorter не может быть null");

        List<RoleAssignment> filtered = findByFilter(filter);
        filtered.sort(sorter);
        return filtered;
    }

    public List<RoleAssignment> findAllParallel(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        Objects.requireNonNull(filter, "filter не может быть null");
        Objects.requireNonNull(sorter, "sorter не может быть null");

        List<RoleAssignment> filtered = findByFilterParallel(filter);
        filtered.sort(sorter);
        return filtered;
    }

    public List<RoleAssignment> getActiveAssignments() {
        synchronized (lock) {
            List<RoleAssignment> result = new ArrayList<>();
            for (RoleAssignment assignment : assignments.values()) {
                if (assignment.isActive()) {
                    result.add(assignment);
                }
            }
            return result;
        }
    }

    public List<RoleAssignment> getExpiredAssignments() {
        synchronized (lock) {
            List<RoleAssignment> result = new ArrayList<>();
            for (RoleAssignment assignment : assignments.values()) {
                if (assignment instanceof TemporaryAssignment temp && temp.isExpired()) {
                    result.add(assignment);
                }
            }
            return result;
        }
    }

    public boolean userHasRole(User user, Role role) {
        Objects.requireNonNull(user, "user не может быть null");
        Objects.requireNonNull(role, "role не может быть null");

        synchronized (lock) {
            for (RoleAssignment assignment : assignments.values()) {
                if (assignment.user().equals(user)
                        && assignment.role().equals(role)
                        && assignment.isActive()) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        Objects.requireNonNull(user, "user не может быть null");
        Objects.requireNonNull(permissionName, "permissionName не может быть null");
        Objects.requireNonNull(resource, "resource не может быть null");

        synchronized (lock) {
            for (RoleAssignment assignment : assignments.values()) {
                if (!assignment.isActive()) {
                    continue;
                }
                if (!assignment.user().equals(user)) {
                    continue;
                }
                Role r = assignment.role();
                if (r.hasPermission(permissionName, resource)) {
                    return true;
                }
            }
            return false;
        }
    }

    public Set<Permission> getUserPermissions(User user) {
        Objects.requireNonNull(user, "user не может быть null");

        synchronized (lock) {
            Set<Permission> permissions = new HashSet<>();
            for (RoleAssignment assignment : assignments.values()) {
                if (assignment.isActive() && assignment.user().equals(user)) {
                    permissions.addAll(assignment.role().getPermissions());
                }
            }
            return permissions;
        }
    }

    public void revokeAssignment(String assignmentId) {
        Objects.requireNonNull(assignmentId, "assignmentId не может быть null");

        synchronized (lock) {
            RoleAssignment removed = assignments.remove(assignmentId);
            if (removed == null) {
                throw new NoSuchElementException("Назначение с id " + assignmentId + " не найдено");
            }
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        Objects.requireNonNull(assignmentId, "assignmentId не может быть null");
        Objects.requireNonNull(newExpirationDate, "newExpirationDate не может быть null");

        synchronized (lock) {
            RoleAssignment assignment = assignments.get(assignmentId);
            if (assignment == null) {
                throw new NoSuchElementException("Назначение с id " + assignmentId + " не найдено");
            }

            if (!(assignment instanceof TemporaryAssignment temp)) {
                throw new IllegalStateException("Назначение с id " + assignmentId + " не является временным");
            }

            temp.extend(newExpirationDate);
        }
    }
}

