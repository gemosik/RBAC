public record User(String username, String fullName, String email) {

    public static User create(String username, String fullName, String email) {
        validateNull(username, "username");
        validateNull(fullName, "fullname");
        validateNull(email,"email");

        if (username.length() < 3 || username.length() > 20) {
            throw new IllegalArgumentException("Username должен быть от 3 до 20 символов");
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username должен содержать только латинские буквы, цифры и подчёркивания");
        }

        if (!email.contains("@") || !email.substring(email.indexOf("@") + 1).contains(".")) {
            throw new IllegalArgumentException("Email  должен соответствовать базовому формату email (содержать @ и точку после @)");
        }

        return new User(username.trim(), fullName.trim(), email.trim());
    }

    private static void validateNull(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " не может быть пустым");
        }
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
