package com.sgitu.userservice.service;

import com.sgitu.userservice.dto.*;
import com.sgitu.userservice.entity.*;
import com.sgitu.userservice.exception.*;
import com.sgitu.userservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserResponseDTO createUser(UserRequestDTO request) {
        if (request.getEmail() == null || request.getEmail().isBlank())
            throw new IllegalArgumentException("L'email est obligatoire");
        if (request.getPassword() == null || request.getPassword().isBlank())
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        if (request.getRole() == null || request.getRole().isBlank())
            throw new IllegalArgumentException("Le role est obligatoire");

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Rôle introuvable : " + request.getRole()));

        UserProfile profile = null;
        if (request.getProfile() != null) {
            profile = UserProfile.builder()
                    .firstName(request.getProfile().getFirstName())
                    .lastName(request.getProfile().getLastName())
                    .phone(request.getProfile().getPhone())
                    .address(request.getProfile().getAddress())
                    .birthDate(request.getProfile().getBirthDate())
                    .build();
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profile(profile)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        User saved = userRepository.save(user);
        // Notify consumers: new user is active
        eventPublisher.publish(saved.getId(), "active");
        return toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUsersByRole(String roleName) {
        return userRepository.findByRolesName(roleName).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException(request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        // Update password when provided (JDBC bypasses Hibernate to avoid dirty-check conflicts)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            jdbcTemplate.update("UPDATE users SET password = ? WHERE id = ?",
                    passwordEncoder.encode(request.getPassword()), id);
        }

        if (request.getProfile() != null) {
            UserProfile profile = user.getProfile();
            if (profile == null) {
                profile = new UserProfile();
                user.setProfile(profile);
            }
            profile.setFirstName(request.getProfile().getFirstName());
            profile.setLastName(request.getProfile().getLastName());
            profile.setPhone(request.getProfile().getPhone());
            profile.setAddress(request.getProfile().getAddress());
            profile.setBirthDate(request.getProfile().getBirthDate());
        }

        return toResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long id, String newPassword) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        String encoded = passwordEncoder.encode(newPassword);
        int updated = jdbcTemplate.update("UPDATE users SET password = ? WHERE id = ?", encoded, id);
        if (updated == 0) {
            throw new UserNotFoundException(id);
        }
    }

    @Override
    public UserResponseDTO updateRoles(Long id, List<String> roleNames) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        Set<Role> roles = roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("Rôle introuvable : " + name)))
                .collect(Collectors.toSet());

        user.setRoles(roles);
        return toResponseDTO(userRepository.save(user));
    }

    @Override
    public UserResponseDTO deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(false);
        UserResponseDTO result = toResponseDTO(userRepository.save(user));
        // Notify consumers: user is now inactive
        eventPublisher.publish(id, "inactive");
        return result;
    }

    @Override
    public UserResponseDTO activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(true);
        UserResponseDTO result = toResponseDTO(userRepository.save(user));
        // Notify consumers: user is active again
        eventPublisher.publish(id, "active");
        return result;
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }


    // ── Mapping helpers ──

    private UserResponseDTO toResponseDTO(User user) {
        ProfileDTO profileDTO = null;
        if (user.getProfile() != null) {
            profileDTO = ProfileDTO.builder()
                    .firstName(user.getProfile().getFirstName())
                    .lastName(user.getProfile().getLastName())
                    .phone(user.getProfile().getPhone())
                    .address(user.getProfile().getAddress())
                    .birthDate(user.getProfile().getBirthDate())
                    .build();
        }

        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .active(user.getActive())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .profile(profileDTO)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
