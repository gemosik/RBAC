import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static AssignmentMetadata now(String assignedBy, String reason){
        validateNull(assignedBy, "assignedBy");
        String currentTime = LocalDateTime.now().format(FORMATTER);
        return new AssignmentMetadata(assignedBy.trim(),
                currentTime,
                reason != null ? reason.trim() : null);
    }

    public String format() {
        return String.format("Assigned by %s at %s%s",
                assignedBy,
                assignedAt,
                reason != null ? " (Reason: " + reason + ")" : "");
    }

    private static void validateNull(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " не может быть пустым");
        }
    }

    public static void main(){
        AssignmentMetadata data = AssignmentMetadata.now("admin","just for fun");
        System.out.println(data.format());

    }
}
