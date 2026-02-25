import java.util.Objects;

@FunctionalInterface
public interface RoleFilter {
    boolean test(Role role);

    default RoleFilter and(RoleFilter other) {
        Objects.requireNonNull(other);
        return (role) -> this.test(role) && other.test(role);
    }

    default RoleFilter or(RoleFilter other) {
        Objects.requireNonNull(other);
        return (role) -> this.test(role) || other.test(role);
    }

}
