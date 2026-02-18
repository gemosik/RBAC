import java.util.*;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {

        if (user == null) {
            throw new IllegalArgumentException("User не может быть null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role не может быть null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata не может быть null");
        }

        this.assignmentId = "assign_" + UUID.randomUUID().toString();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return Objects.equals(assignmentId, that.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    public String summary() {
        String type = assignmentType();

        String status = isActive() ? "ACTIVE" : "INACTIVE";

        String result = String.format("[%s] %s assigned to %s by %s at %s",
                type,
                role.getName(),
                user.username(),
                metadata.assignedBy(),
                metadata.assignedAt()
        );

        if (metadata.reason() != null && !metadata.reason().isEmpty()) {
            result += String.format("\nReason: %s", metadata.reason());
        }

        result += String.format("\nStatus: %s", status);

        return result;
    }

}
