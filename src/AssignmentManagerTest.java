import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AssignmentManagerTest {

    private AssignmentManager createManagerWithUserAndRole(User user, Role role) {
        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        userManager.add(user);
        roleManager.add(role);
        return new AssignmentManager(userManager, roleManager);
    }

    @Test
    void addAssignmentAndFindByUserAndRole() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        AssignmentManager manager = createManagerWithUserAndRole(user, role);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        RoleAssignment assignment = new PermanentAssignment(user, role, metadata);

        manager.add(assignment);

        assertEquals(1, manager.count());

        assertEquals(1, manager.findByUser(user).size());
        assertEquals(1, manager.findByRole(role).size());
    }

    @Test
    void addAssignmentWithUnknownUserThrows() {
        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        Role role = new Role("ADMIN", "Admin role");
        roleManager.add(role);

        AssignmentManager manager = new AssignmentManager(userManager, roleManager);

        User user = User.create("john", "John Doe", "john@example.com");
        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        RoleAssignment assignment = new PermanentAssignment(user, role, metadata);

        assertThrows(IllegalStateException.class, () -> manager.add(assignment));
    }

    @Test
    void addDuplicateActiveAssignmentThrows() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        AssignmentManager manager = createManagerWithUserAndRole(user, role);

        AssignmentMetadata metadata1 = AssignmentMetadata.now("system", "first");
        AssignmentMetadata metadata2 = AssignmentMetadata.now("system", "second");
        RoleAssignment a1 = new PermanentAssignment(user, role, metadata1);
        RoleAssignment a2 = new PermanentAssignment(user, role, metadata2);

        manager.add(a1);
        assertThrows(IllegalStateException.class, () -> manager.add(a2));
    }

    @Test
    void getActiveAndExpiredAssignments() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        AssignmentManager manager = createManagerWithUserAndRole(user, role);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        RoleAssignment active = new PermanentAssignment(user, role, metadata);
        RoleAssignment expiredTemp = new TemporaryAssignment(
                user, role, metadata, "2000-01-01", false
        );

        manager.add(active);
        manager.add(expiredTemp);

        assertEquals(1, manager.getActiveAssignments().size());
        assertEquals(1, manager.getExpiredAssignments().size());
    }

    @Test
    void revokeExpiredTemporaryAssignmentsRevokesOnce() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        AssignmentManager manager = createManagerWithUserAndRole(user, role);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        TemporaryAssignment expiredTemp = new TemporaryAssignment(
                user, role, metadata, "2000-01-01", false
        );
        manager.add(expiredTemp);

        assertEquals(1, manager.revokeExpiredTemporaryAssignments());
        assertTrue(expiredTemp.isRevoked());
        assertEquals(0, manager.revokeExpiredTemporaryAssignments());
    }

    @Test
    void userHasRoleAndPermission() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        Permission perm = new Permission("READ", "users", "read users");
        role.addPermission(perm);

        AssignmentManager manager = createManagerWithUserAndRole(user, role);
        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        RoleAssignment assignment = new PermanentAssignment(user, role, metadata);
        manager.add(assignment);

        assertTrue(manager.userHasRole(user, role));
        assertTrue(manager.userHasPermission(user, "READ", "users"));
        assertFalse(manager.userHasPermission(user, "WRITE", "users"));
    }

    @Test
    void getUserPermissionsAggregatesFromAllRoles() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role admin = new Role("ADMIN", "Admin role");
        Role editor = new Role("EDITOR", "Editor role");

        Permission readUsers = new Permission("READ", "users", "read users");
        Permission writeUsers = new Permission("WRITE", "users", "write users");
        admin.addPermission(readUsers);
        editor.addPermission(writeUsers);

        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        userManager.add(user);
        roleManager.add(admin);
        roleManager.add(editor);

        AssignmentManager manager = new AssignmentManager(userManager, roleManager);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        RoleAssignment a1 = new PermanentAssignment(user, admin, metadata);
        RoleAssignment a2 = new PermanentAssignment(user, editor, metadata);
        manager.add(a1);
        manager.add(a2);

        Set<Permission> permissions = manager.getUserPermissions(user);
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains(readUsers));
        assertTrue(permissions.contains(writeUsers));
    }

    @Test
    void revokeAssignmentRemovesIt() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        AssignmentManager manager = createManagerWithUserAndRole(user, role);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        RoleAssignment assignment = new PermanentAssignment(user, role, metadata);
        manager.add(assignment);

        String id = assignment.assignmentId();
        manager.revokeAssignment(id);

        assertEquals(0, manager.count());
        assertTrue(manager.findById(id).isEmpty());
    }

    @Test
    void revokeMissingAssignmentThrows() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        AssignmentManager manager = createManagerWithUserAndRole(user, role);

        assertThrows(NoSuchElementException.class,
                () -> manager.revokeAssignment("missing"));
    }

    @Test
    void extendTemporaryAssignmentChangesExpiration() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        AssignmentManager manager = createManagerWithUserAndRole(user, role);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        TemporaryAssignment temp = new TemporaryAssignment(
                user, role, metadata, "3000-01-01", false
        );
        manager.add(temp);

        String id = temp.assignmentId();
        manager.extendTemporaryAssignment(id, "3001-01-01");

        assertEquals("3001-01-01 23:59", temp.getExpiresAt());
    }

    @Test
    void extendNonExistingOrNonTemporaryThrows() {
        User user = User.create("john", "John Doe", "john@example.com");
        Role role = new Role("ADMIN", "Admin role");
        AssignmentManager manager = createManagerWithUserAndRole(user, role);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        RoleAssignment permanent = new PermanentAssignment(user, role, metadata);
        manager.add(permanent);

        assertThrows(NoSuchElementException.class,
                () -> manager.extendTemporaryAssignment("missing", "3001-01-01"));

        assertThrows(IllegalStateException.class,
                () -> manager.extendTemporaryAssignment(permanent.assignmentId(), "3001-01-01"));
    }
}

