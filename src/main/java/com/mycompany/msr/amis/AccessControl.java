package com.mycompany.msr.amis;

public final class AccessControl {

    public static final String PRIMARY_SUPER_ADMIN_EMAIL = "wkautsa@gmail.com";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";

    private AccessControl() {
    }

    public static void requireRole(String... allowedRoles) {
        if (!Session.hasRole(allowedRoles)) {
            throw new SecurityException("Access denied.");
        }
    }

    public static boolean canManageUsers() {
        return Session.hasRole(ROLE_SUPER_ADMIN, ROLE_ADMIN);
    }

    public static boolean canAssignRole(String role) {
        if (Session.hasRole(ROLE_SUPER_ADMIN)) {
            return ROLE_ADMIN.equalsIgnoreCase(role)
                    || ROLE_USER.equalsIgnoreCase(role);
        }
        if (Session.hasRole(ROLE_ADMIN)) {
            return ROLE_USER.equalsIgnoreCase(role);
        }
        return false;
    }

    public static boolean canManageTarget(User target) {
        if (target == null || Session.getCurrentUser() == null) {
            return false;
        }
        if (Session.hasRole(ROLE_SUPER_ADMIN)) {
            return true;
        }
        return Session.hasRole(ROLE_ADMIN) && !ROLE_SUPER_ADMIN.equalsIgnoreCase(target.getRole());
    }

    public static boolean isProtectedSuperAdmin(User user) {
        return user != null && ROLE_SUPER_ADMIN.equalsIgnoreCase(user.getRole());
    }

    public static boolean isPrimarySuperAdminEmail(String email) {
        return email != null && PRIMARY_SUPER_ADMIN_EMAIL.equalsIgnoreCase(email.trim());
    }

    public static boolean isPrimarySuperAdmin(User user) {
        return user != null && isPrimarySuperAdminEmail(user.getEmail());
    }
}
