public record User(String username, String fullName, String email) {

    public static User create(String username, String fullName, String email) {
        ValidationUtils.requireNonEmpty(username, "username");
        ValidationUtils.requireNonEmpty(fullName, "fullName");
        ValidationUtils.requireNonEmpty(email, "email");

        String normalizedUsername = ValidationUtils.normalizeString(username);
        if (!ValidationUtils.isValidUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username должен быть 3–20 символов и содержать только латинские буквы, цифры и подчёркивания");
        }

        String normalizedEmail = ValidationUtils.normalizeString(email).toLowerCase();
        if (!ValidationUtils.isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email должен соответствовать формату email");
        }

        String normalizedFullName = ValidationUtils.normalizeString(fullName);
        return new User(normalizedUsername, normalizedFullName, normalizedEmail);
    }

    public String format(){
        return String.format("%s (%s) <%s>", username, fullName, email);
    }

    public static void main(){
        User.create("slayzer","Nail","gadirov2005@mail.ru");
        User.create("dfg","Nail","gadirov2005@mail.ru");
        User.create("er","ав","ав@mail.ru");
    }
}
