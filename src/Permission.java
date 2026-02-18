public record Permission(String name, String resource, String description) {

    public Permission(String name, String resource, String description) {
        validateNull(name, "name");
        validateNull(resource, "resource");
        validateNull(description,"description");

        String normalizedName = name.trim().toUpperCase();
        if (normalizedName.contains(" ")) {
            throw new IllegalArgumentException("Name не может содержать пробелы");
        }

        String normalizedResource = resource.trim().toLowerCase();
        String normalizedDescription = description.trim();

        this.name = normalizedName;
        this.resource = normalizedResource;
        this.description = normalizedDescription;
    }

    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        if (namePattern == null || resourcePattern == null) {
            return false;
        }
        return name.contains(namePattern.toUpperCase()) &&
                resource.contains(resourcePattern.toLowerCase());
    }

    private static void validateNull(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " не может быть пустым");
        }
    }

    public static void main(){
        Permission oleg = new Permission("fdfd","jJjjj","klkl");
        System.out.println(oleg.format());
    }
}