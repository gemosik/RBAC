import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserManagerTest {

    @Test
    void addAndFindUserByUsername() {
        UserManager manager = new UserManager();
        User user = User.create("john", "John Doe", "john@example.com");

        manager.add(user);

        assertEquals(1, manager.count());
        assertTrue(manager.exists("john"));

        Optional<User> found = manager.findByUsername("john");
        assertTrue(found.isPresent());
        assertEquals("john", found.get().username());
    }

    @Test
    void addDuplicateUsernameThrows() {
        UserManager manager = new UserManager();
        User user1 = User.create("john", "John Doe", "john@example.com");
        User user2 = User.create("john", "John Smith", "john2@example.com");

        manager.add(user1);
        assertThrows(IllegalArgumentException.class, () -> manager.add(user2));
    }

    @Test
    void findByEmailWorks() {
        UserManager manager = new UserManager();
        User user = User.create("john", "John Doe", "john@example.com");
        manager.add(user);

        Optional<User> found = manager.findByEmail("john@example.com");
        assertTrue(found.isPresent());
        assertEquals("john", found.get().username());
    }

    @Test
    void findByFilterAndSortingWorks() {
        UserManager manager = new UserManager();
        manager.add(User.create("alice", "Alice A", "a@example.com"));
        manager.add(User.create("bob", "Bob B", "b@example.com"));
        manager.add(User.create("charlie", "Charlie C", "c@example.com"));

        UserFilter filter = user ->
                user.username().startsWith("b") || user.username().startsWith("c");

        List<User> filtered = manager.findAll(filter, Comparator.comparing(User::username));

        assertEquals(2, filtered.size());
        assertEquals("bob", filtered.get(0).username());
        assertEquals("charlie", filtered.get(1).username());
    }

    @Test
    void updateExistingUserChangesData() {
        UserManager manager = new UserManager();
        manager.add(User.create("john", "John Doe", "john@example.com"));

        manager.update("john", "John Updated", "john.new@example.com");

        Optional<User> found = manager.findByUsername("john");
        assertTrue(found.isPresent());
        assertEquals("John Updated", found.get().fullName());
        assertEquals("john.new@example.com", found.get().email());
    }

    @Test
    void updateNonExistingUserThrows() {
        UserManager manager = new UserManager();

        assertThrows(NoSuchElementException.class,
                () -> manager.update("missing", "Name", "email@example.com"));
    }

    @Test
    void removeUserDecreasesCount() {
        UserManager manager = new UserManager();
        User user = User.create("john", "John Doe", "john@example.com");
        manager.add(user);

        boolean removed = manager.remove(user);

        assertTrue(removed);
        assertEquals(0, manager.count());
        assertFalse(manager.exists("john"));
    }
}

