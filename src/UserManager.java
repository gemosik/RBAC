import java.util.*;

public class UserManager implements Repository<User> {

    private final Map<String, User> storage = new HashMap<>();

    @Override
    public void add(User user) {
        Objects.requireNonNull(user, "user не может быть null");
        String username = user.username();

        if (storage.containsKey(username)) {
            throw new IllegalArgumentException("Пользователь с таким username уже существует: " + username);
        }

        User validated = User.create(user.username(), user.fullName(), user.email());
        storage.put(validated.username(), validated);
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        return storage.remove(user.username(), user);
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public int count() {
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(username));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }

        return storage.values().stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        Objects.requireNonNull(filter, "filter не может быть null");

        List<User> result = new ArrayList<>();
        for (User user : storage.values()) {
            if (filter.test(user)) {
                result.add(user);
            }
        }
        return result;
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        Objects.requireNonNull(filter, "filter не может быть null");
        Objects.requireNonNull(sorter, "sorter не может быть null");

        List<User> filtered = findByFilter(filter);
        filtered.sort(sorter);
        return filtered;
    }

    public boolean exists(String username) {
        if (username == null) {
            return false;
        }
        return storage.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        Objects.requireNonNull(username, "username не может быть null");

        User existing = storage.get(username);
        if (existing == null) {
            throw new NoSuchElementException("Пользователь с username " + username + " не найден");
        }

        User updated = User.create(username, newFullName, newEmail);
        storage.put(username, updated);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserManager that = (UserManager) o;
        return Objects.equals(storage, that.storage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storage);
    }
}
