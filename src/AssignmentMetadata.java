public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    public static AssignmentMetadata now(String assignedBy, String reason){
        ValidationUtils.requireNonEmpty(assignedBy, "assignedBy");
        String currentTime = new DateUtils().getCurrentDateTime();
        return new AssignmentMetadata(ValidationUtils.normalizeString(assignedBy),
                currentTime,
                reason != null ? ValidationUtils.normalizeString(reason) : null);
    }

    public String format() {
        return String.format("Assigned by %s at %s%s",
                assignedBy,
                assignedAt,
                reason != null ? " (Reason: " + reason + ")" : "");
    }

    public static void main(){
        AssignmentMetadata data = AssignmentMetadata.now("admin","just for fun");
        System.out.println(data.format());

    }
}
