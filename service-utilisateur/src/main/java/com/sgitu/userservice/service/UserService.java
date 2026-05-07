package com.sgitu.userservice.service;

import com.sgitu.userservice.dto.*;

import java.util.List;

public interface UserService {

    UserResponseDTO createUser(UserRequestDTO request);

    UserResponseDTO getUserById(Long id);

    List<UserResponseDTO> getAllUsers();

    List<UserResponseDTO> getUsersByRole(String roleName);

    UserResponseDTO updateUser(Long id, UserRequestDTO request);

    void changePassword(Long id, String newPassword);

    UserResponseDTO updateRoles(Long id, List<String> roleNames);

    UserResponseDTO deactivateUser(Long id);

    UserResponseDTO activateUser(Long id);

    void deleteUser(Long id);

    boolean userExists(Long id);
}
