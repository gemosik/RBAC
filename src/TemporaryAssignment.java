import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import java.util.*;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private String expiresAt;
    private boolean autoRenew;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata, String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        validateExpirationDate(expiresAt);
        this.expiresAt = normalizeDateTime(expiresAt);
        this.autoRenew = autoRenew;
    }

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt) {
        this(user, role, metadata, expiresAt, false);
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = parseDateTime(expiresAt);
        return now.isAfter(expiration);
    }
    
    public void extend(String newExpirationDate) {
        validateExpirationDate(newExpirationDate);
        String normalizedDate = normalizeDateTime(newExpirationDate);

        LocalDateTime currentExpiration = parseDateTime(expiresAt);
        LocalDateTime newExpiration = parseDateTime(normalizedDate);

        if (!newExpiration.isAfter(currentExpiration)) {
            throw new IllegalArgumentException("Новая дата истечения должна быть позже текущей");
        }

        this.expiresAt = normalizedDate;
    }

    public String getTimeRemaining() {
        if (isExpired()) {
            return "Истекло";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = parseDateTime(expiresAt);

        long days = ChronoUnit.DAYS.between(now, expiration);
        long hours = ChronoUnit.HOURS.between(now, expiration) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, expiration) % 60;

        if (days > 0) {
            return String.format("%d дн %d ч %d мин", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d ч %d мин", hours, minutes);
        } else {
            return String.format("%d мин", minutes);
        }
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.parse(dateTimeStr + " 23:59", FORMATTER);
        }
    }

    private String normalizeDateTime(String dateTimeStr) {
        if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return dateTimeStr + " 23:59";
        }
        return dateTimeStr;
    }

    private void validateExpirationDate(String expiresAt) {
        if (expiresAt == null || expiresAt.trim().isEmpty()) {
            throw new IllegalArgumentException("Дата истечения не может быть пустой");
        }

        try {
            parseDateTime(expiresAt);
        } catch (Exception e) {
            throw new IllegalArgumentException("Неверный формат даты. Используйте YYYY-MM-DD или YYYY-MM-DD HH:MM");
        }
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();
        String status = isActive() ? "ACTIVE" : "EXPIRED";
        String renewStatus = autoRenew ? "ENABLED" : "DISABLED";
        String timeRemaining = isActive() ? getTimeRemaining() : "N/A";

        StringBuilder sb = new StringBuilder(baseSummary);

        int statusIndex = sb.lastIndexOf("Status:");
        if (statusIndex != -1) {
            sb.replace(statusIndex, sb.length(), String.format("Status: %s", status));
        }

        sb.append(String.format("\nExpires: %s", expiresAt));
        sb.append(String.format("\nAuto-renew: %s", renewStatus));

        if (isActive()) {
            sb.append(String.format("\nTime remaining: %s", timeRemaining));
        }

        return sb.toString();
    }
 }