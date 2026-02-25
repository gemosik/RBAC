import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RoleManagerTest {

    @Test
    void addAndFindByName() {
        RoleManager manager = new RoleManager();
        Role role = new Role("ADMIN", "Admin role");

        manager.add(role);

        assertEquals(1, manager.count());

        Optional<Role> found = manager.findByName("ADMIN");
        assertTrue(found.isPresent());
        assertEquals("ADMIN", found.get().getName());
    }

    @Test
    void addDuplicateNameThrows() {
        RoleManager manager = new RoleManager();
        Role role1 = new Role("ADMIN", "Admin role");
        Role role2 = new Role("ADMIN", "Another admin");

        manager.add(role1);
        assertThrows(IllegalArgumentException.class, () -> manager.add(role2));
    }

    @Test
    void findByFilterAndSortingWorks() {
        RoleManager manager = new RoleManager();
        Role r1 = new Role("ADMIN", "Admin role");
        Role r2 = new Role("USER", "User role");
        Role r3 = new Role("GUEST", "Guest role");

        manager.add(r1);
        manager.add(r2);
        manager.add(r3);

        RoleFilter filter = role -> role.getName().contains("E");
        List<Role> filtered = manager.findAll(filter, Comparator.comparing(Role::getName));

        assertEquals(2, filtered.size());
        assertEquals("GUEST", filtered.get(0).getName());
        assertEquals("USER", filtered.get(1).getName());
    }

    @Test
    void addAndRemovePermission() {
        RoleManager manager = new RoleManager();
        Role role = new Role("ADMIN", "Admin role");
        manager.add(role);

        Permission perm = new Permission("READ", "users", "read users");
        manager.addPermissionToRole("ADMIN", perm);
        assertTrue(role.hasPermission(perm));

        manager.removePermissionFromRole("ADMIN", perm);
        assertFalse(role.hasPermission(perm));
    }

    @Test
    void removeAssignedRoleThrows() {
        RoleManager manager = new RoleManager();
        Role role = new Role("ADMIN", "Admin role");
        manager.add(role);

        User user = User.create("john", "John Doe", "john@example.com");
        AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
        RoleAssignment assignment = new PermanentAssignment(user, role, metadata);

        manager.setAssignments(List.of(assignment));

        assertThrows(IllegalStateException.class, () -> manager.remove(role));
    }

    @Test
    void findRolesWithPermissionWorks() {
        RoleManager manager = new RoleManager();
        Role admin = new Role("ADMIN", "Admin role");
        Role user = new Role("USER", "User role");

        Permission readUsers = new Permission("READ", "users", "read users");
        admin.addPermission(readUsers);
        user.addPermission(new Permission("READ", "posts", "read posts"));

        manager.add(admin);
        manager.add(user);

        List<Role> result = manager.findRolesWithPermission("READ", "users");
        assertEquals(1, result.size());
        assertEquals("ADMIN", result.get(0).getName());
    }

    @Test
    void addPermissionToMissingRoleThrows() {
        RoleManager manager = new RoleManager();
        Permission perm = new Permission("READ", "users", "read users");

        assertThrows(NoSuchElementException.class,
                () -> manager.addPermissionToRole("MISSING", perm));
    }
}

