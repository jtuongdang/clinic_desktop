package vn.clinicmanagement.application.auth;

public record AuthenticatedUser(int userId, String username, String fullName, String roleCode, String roleName) {}
