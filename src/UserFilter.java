import java.util.Objects;

@FunctionalInterface
public interface UserFilter {
    boolean test(User user);

    default UserFilter and(UserFilter other) {
        Objects.requireNonNull(other);
        return (user) -> this.test(user) && other.test(user);
    }

    default UserFilter or(UserFilter other) {
        Objects.requireNonNull(other);
        return (user) -> this.test(user) || other.test(user);
    }
}
