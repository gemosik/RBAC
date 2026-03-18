public record Permission(String name, String resource, String description) {

    public Permission(String name, String resource, String description) {
        ValidationUtils.requireNonEmpty(name, "name");
        ValidationUtils.requireNonEmpty(resource, "resource");
        ValidationUtils.requireNonEmpty(description,"description");

        String normalizedName = ValidationUtils.normalizeString(name).toUpperCase();
        if (normalizedName.contains(" ")) {
            throw new IllegalArgumentException("Name не может содержать пробелы");
        }

        String normalizedResource = ValidationUtils.normalizeString(resource).toLowerCase();
        String normalizedDescription = ValidationUtils.normalizeString(description);

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

    public static void main(){
        Permission oleg = new Permission("fdfd","jJjjj","klkl");
        System.out.println(oleg.format());
    }
}