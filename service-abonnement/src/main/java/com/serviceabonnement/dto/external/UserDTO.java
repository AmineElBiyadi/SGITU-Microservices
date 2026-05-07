package com.serviceabonnement.dto.external;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private boolean active;
    private List<String> roles;
    private UserProfile profile;
    private LocalDateTime createdAt;

    @Data
    public static class UserProfile {
        private String firstName;
        private String lastName;
        private String phone;
        private String address;
        private String birthDate;
    }
}
