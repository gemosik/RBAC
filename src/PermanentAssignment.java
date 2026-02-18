import java.util.*;

public class PermanentAssignment extends AbstractRoleAssignment {
    private boolean revoked;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
        this.revoked = false;
    }

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata, boolean revoked) {
        super(user, role, metadata);
        this.revoked = revoked;
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void restore() {
        this.revoked = false;
    }

    @Override
    public String summary() {

        String type = assignmentType();
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        String revokedStatus = revoked ? "YES" : "NO";

        String result = String.format("[%s] %s assigned to %s by %s at %s",
                type,
                role().getName(),
                user().username(),
                metadata().assignedBy(),
                metadata().assignedAt()
        );

        if (metadata().reason() != null && !metadata().reason().isEmpty()) {
            result += String.format("\nReason: %s", metadata().reason());
        }

        result += String.format("\nStatus: %s", status);
        result += String.format("\nRevoked: %s", revokedStatus);

        return result;
    }
}
