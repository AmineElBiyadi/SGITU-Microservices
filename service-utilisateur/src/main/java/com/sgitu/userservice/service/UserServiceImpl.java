package com.sgitu.userservice.service;

import com.sgitu.userservice.dto.*;
import com.sgitu.userservice.entity.*;
import com.sgitu.userservice.exception.*;
import com.sgitu.userservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher eventPublisher;
    private final KafkaNotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;
    private final EmailVerificationService verificationService;

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
                .active(false)  // User starts inactive until email is verified
                .build();

        User saved = userRepository.save(user);
        
        // Generate and store verification code
        String verificationCode = verificationService.generateVerificationCode();
        verificationService.storeVerificationCode(saved.getEmail(), verificationCode);
        
        // Log the code for testing purposes (REMOVE IN PRODUCTION)
        log.info("🔐 VERIFICATION CODE FOR {} : {}", saved.getEmail(), verificationCode);
        
        // Send verification email via Kafka (G5)
        notificationService.sendVerificationEmail(saved, verificationCode);
        
        log.info("User created (inactive) - email verification required: {}", saved.getEmail());
        
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
    @Transactional(readOnly = true)
    public List<Long> getDriverIds() {
        return userRepository.findIdsByRolesName("ROLE_DRIVER");
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
        User saved = userRepository.save(user);
        UserResponseDTO result = toResponseDTO(saved);
        
        // Notify Group 8 (Analytics) via Kafka
        eventPublisher.publish(id, "inactive");
        
        // Notify Group 5 (Notifications) via Kafka
        notificationService.sendNotification("ACCOUNT_DEACTIVATED", saved);
        
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

        @Override
        @Transactional(readOnly = true)
        public NotificationRecipientsResponseDTO getNotificationRecipients(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findByActiveTrueAndEmailIsNotNull(pageable);

        List<NotificationRecipientDTO> items = userPage.getContent().stream()
            .map(u -> NotificationRecipientDTO.builder()
                .userId(u.getId())
                .email(u.getEmail())
                .build())
            .collect(Collectors.toList());

        return NotificationRecipientsResponseDTO.builder()
            .items(items)
            .page(userPage.getNumber())
            .size(userPage.getSize())
            .total(userPage.getTotalElements())
            .build();
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

    // ── Email Verification Methods ──

    @Override
    public void verifyEmail(String email, String code) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Check if already verified
        if (user.getActive()) {
            throw new IllegalStateException("This account is already verified");
        }

        // Verify the code
        boolean isValid = verificationService.verifyCode(email, code);
        
        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        // Activate the user
        user.setActive(true);
        userRepository.save(user);

        // Publish event to G8 (Analytics)
        eventPublisher.publish(user.getId(), "active");

        // Send welcome email via Kafka (G5)
        notificationService.sendNotification("WELCOME", user);

        log.info("Email successfully verified for: {}", email);
    }

    @Override
    public void resendVerificationCode(String email) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Check if already verified
        if (user.getActive()) {
            throw new IllegalStateException("This account is already verified");
        }

        // Generate new code
        String newCode = verificationService.generateVerificationCode();
        verificationService.storeVerificationCode(email, newCode);

        // Log for testing (REMOVE IN PRODUCTION)
        log.info("🔐 NEW VERIFICATION CODE FOR {} : {}", email, newCode);

        // Send new code by email
        notificationService.sendVerificationEmail(user, newCode);

        log.info("New verification code sent to: {}", email);
    }
}
