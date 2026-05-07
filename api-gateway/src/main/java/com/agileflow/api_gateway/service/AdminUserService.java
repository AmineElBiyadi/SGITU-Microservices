package com.agileflow.api_gateway.service;

import com.agileflow.api_gateway.dto.AdminUserResponse;
import com.agileflow.api_gateway.model.User;
import com.agileflow.api_gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        return AdminUserResponse.from(findUser(id));
    }

    public AdminUserResponse changeRole(Long id, String role) {
        User user = findUser(id);
        user.setRole(parseRole(role));
        tokenService.revokeAllUserTokens(user);
        return AdminUserResponse.from(userRepository.save(user));
    }

    public AdminUserResponse updateStatus(Long id, boolean enabled) {
        User user = findUser(id);
        user.setEnabled(enabled);

        if (!enabled) {
            tokenService.revokeAllUserTokens(user);
        }

        return AdminUserResponse.from(userRepository.save(user));
    }

    public AdminUserResponse updateEmailVerification(Long id, boolean emailVerified) {
        User user = findUser(id);
        user.setEmailVerified(emailVerified);

        if (!emailVerified) {
            user.setEnabled(false);
            tokenService.revokeAllUserTokens(user);
        }

        return AdminUserResponse.from(userRepository.save(user));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private User.RoleType parseRole(String role) {
        try {
            return User.RoleType.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role invalide");
        }
    }
}
