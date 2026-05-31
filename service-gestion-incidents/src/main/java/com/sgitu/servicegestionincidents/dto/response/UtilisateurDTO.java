package com.sgitu.servicegestionincidents.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilisateurDTO {
    private Long id;
    private String email;
    private Boolean active;
    private List<String> roles;
    private Profile profile;
    private String createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Profile {
        private String firstName;
        private String lastName;
        private String phone;
        private String address;
        private String birthDate;
    }
}
