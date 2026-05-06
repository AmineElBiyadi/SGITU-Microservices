package com.agileflow.api_gateway.controller;

import com.agileflow.api_gateway.dto.AdminUserResponse;
import com.agileflow.api_gateway.dto.ChangeUserRoleRequest;
import com.agileflow.api_gateway.dto.UpdateEmailVerificationRequest;
import com.agileflow.api_gateway.dto.UpdateUserStatusRequest;
import com.agileflow.api_gateway.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Administration G10", description = "Endpoints admin propres a l'authentification G10")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Lister les comptes d'authentification G10")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(adminUserService.listUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un compte d'authentification G10")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUser(id));
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Changer le role d'un compte G10")
    public ResponseEntity<AdminUserResponse> changeRole(@PathVariable Long id,
                                                        @RequestBody ChangeUserRoleRequest request) {
        return ResponseEntity.ok(adminUserService.changeRole(id, request.getRole()));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Activer ou desactiver un compte G10")
    public ResponseEntity<AdminUserResponse> updateStatus(@PathVariable Long id,
                                                          @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(adminUserService.updateStatus(id, request.isEnabled()));
    }

    @PutMapping("/{id}/email-verification")
    @Operation(summary = "Marquer l'email d'un compte G10 comme verifie ou non verifie")
    public ResponseEntity<AdminUserResponse> updateEmailVerification(@PathVariable Long id,
                                                                     @RequestBody UpdateEmailVerificationRequest request) {
        return ResponseEntity.ok(adminUserService.updateEmailVerification(id, request.isEmailVerified()));
    }
}
