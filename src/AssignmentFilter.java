import java.util.Objects;

@FunctionalInterface
public interface AssignmentFilter {
    boolean test(RoleAssignment assignment);

    default AssignmentFilter and(AssignmentFilter other) {
        Objects.requireNonNull(other);
        return (assignment) -> this.test(assignment) && other.test(assignment);
    }

    default AssignmentFilter or(AssignmentFilter other) {
        Objects.requireNonNull(other);
        return (assignment) -> this.test(assignment) || other.test(assignment);
    }

}
