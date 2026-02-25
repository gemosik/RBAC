public class RoleFilters {

    RoleFilter byName(String name){
        return role -> role.getName().equals(name);
    }

    RoleFilter byNameContains(String substring){
        return role -> role.getName().toLowerCase().contains(substring.toLowerCase());
    }

    RoleFilter hasPermission(Permission permission){
        return role -> role.hasPermission(permission);
    }

    RoleFilter hasPermission(String permissionName, String resource){
        return role -> role.hasPermission(permissionName, resource);
    }

    RoleFilter hasAtLeastNPermissions(int n){
        if (n < 0) {
            throw new IllegalArgumentException("Количество не может быть отрицательным");
        }

        return role -> role.getPermissions().size() >= n;
    }

}
