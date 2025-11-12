package raven.modal.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ModelUser {
    private Integer userId;
    private String fullName;
    private String email;
    private String contactNo;

    public ModelUser(String userName, String mail, Role role) {
        this.userName = userName;
        this.role = role;
    }

    private String userName;
    private String password;
    private Role role;

    public enum Role {
        ADMIN, CASHIER, STAFF;

        @Override
        public String toString() {
            switch (this) {
                case ADMIN: return "Admin";
                case CASHIER: return "Cashier";
                default: return super.toString();
            }
        }

        // Converts string (from DB) to enum safely
        public static Role fromString(String value) {
            if (value == null) return null;
            for (Role role : Role.values()) {
                if (role.name().equalsIgnoreCase(value)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unknown role: " + value);
        }
    }
}
