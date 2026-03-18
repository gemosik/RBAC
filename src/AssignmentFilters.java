public class AssignmentFilters {

    public static AssignmentFilter byUser(User user) {
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return assignment -> assignment.user().username().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return assignment -> assignment.role().getName().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return assignment -> assignment.assignmentType().equalsIgnoreCase(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return assignment -> assignment.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String dateTime) {
        return assignment -> assignment.metadata().assignedAt().compareTo(dateTime) > 0;
    }

    public static AssignmentFilter expiringBefore(String dateTime) {
        return assignment -> {
            if (!(assignment instanceof TemporaryAssignment temp)) {
                return false;
            }

            String expiresAt = temp.getExpiresAt();

            ValidationUtils.requireNonEmpty(dateTime, "dateTime");
            String normalizedInput = normalizeDateTimeLikeTemporary(ValidationUtils.normalizeString(dateTime));
            if (!ValidationUtils.isValidDate(normalizedInput)) {
                throw new IllegalArgumentException("Неверный формат даты. Используйте YYYY-MM-DD или YYYY-MM-DD HH:MM");
            }

            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            java.time.LocalDateTime expiration =
                    java.time.LocalDateTime.parse(expiresAt, formatter);
            java.time.LocalDateTime limit =
                    java.time.LocalDateTime.parse(normalizedInput, formatter);

            return expiration.isBefore(limit);
        };
    }

    private static String normalizeDateTimeLikeTemporary(String dateTimeStr) {
        if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return dateTimeStr + " 23:59";
        }
        return dateTimeStr;
    }
}

