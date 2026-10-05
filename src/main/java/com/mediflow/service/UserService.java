package com.mediflow.service;

import com.mediflow.dto.UserDto;
import com.mediflow.dto.UserRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.entity.Role;
import com.mediflow.entity.User;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.RoleRepository;
import com.mediflow.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ASSIGNABLE_ROLES = Set.of(
            Role.PHARMACIST, Role.STORE_KEEPER, Role.PROCUREMENT_OFFICER,
            Role.CASHIER, Role.CRO, Role.FINANCE_MANAGER, Role.ADMIN);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public PageResponse<UserDto> search(String search, String roleName, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firstName"));
        return PageResponse.from(userRepository.search(normalize(search), roleName, active, pageable)
                .map(UserDto::from));
    }

    public UserDto getById(Long id) {
        return UserDto.from(findUser(id));
    }

    @Transactional
    public UserDto create(UserRequest req) {
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw ApiException.badRequest("Temporary password must be at least 8 characters");
        }
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw ApiException.conflict("A user with this email already exists");
        }
        Role role = resolveRole(req.getRoleName());

        User user = new User();
        applyRequest(user, req, role);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setActive(req.getActive() == null || req.getActive());
        User saved = userRepository.save(user);

        auditService.log("USER_CREATED", "USER", saved.getEmail(),
                "Created staff account with role " + role.getName());
        return UserDto.from(saved);
    }

    @Transactional
    public UserDto update(Long id, UserRequest req) {
        User user = findUser(id);
        Role role = resolveRole(req.getRoleName());

        String oldEmail = user.getEmail();
        if (!oldEmail.equalsIgnoreCase(req.getEmail())
                && userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw ApiException.conflict("A user with this email already exists");
        }

        boolean isSelf = user.getId().equals(currentUserId());
        if (isSelf && !user.getRole().getName().equals(role.getName())) {
            throw ApiException.badRequest("You cannot change your own role");
        }
        if (isSelf && Boolean.FALSE.equals(req.getActive())) {
            throw ApiException.badRequest("You cannot deactivate your own account");
        }

        applyRequest(user, req, role);
        if (req.getActive() != null) {
            user.setActive(req.getActive());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (req.getPassword().length() < 8) {
                throw ApiException.badRequest("Password must be at least 8 characters");
            }
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        User saved = userRepository.save(user);

        auditService.log("USER_UPDATED", "USER", saved.getEmail(),
                oldEmail.equals(saved.getEmail()) ? "Profile/role updated" : "Email changed from " + oldEmail);
        return UserDto.from(saved);
    }

    @Transactional
    public UserDto setActive(Long id, boolean active) {
        User user = findUser(id);
        if (user.getId().equals(currentUserId())) {
            throw ApiException.badRequest("You cannot deactivate your own account");
        }
        user.setActive(active);
        User saved = userRepository.save(user);
        auditService.log(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER",
                saved.getEmail(), active ? "Account enabled" : "Account disabled");
        return UserDto.from(saved);
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User me = currentUser();
        if (!passwordEncoder.matches(currentPassword, me.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw ApiException.badRequest("New password must be at least 8 characters");
        }
        me.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(me);
        auditService.log("PASSWORD_CHANGED", "USER", me.getEmail(), "User changed own password");
    }

    public User currentUser() {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Current user not found"));
    }

    public Long currentUserId() {
        try {
            return currentUser().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private void applyRequest(User user, UserRequest req, Role role) {
        user.setFirstName(req.getFirstName().trim());
        user.setLastName(req.getLastName().trim());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setRole(role);
    }

    private Role resolveRole(String roleName) {
        if (roleName == null) throw ApiException.badRequest("Role is required");
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> ApiException.badRequest("Unknown role: " + roleName));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found: " + id));
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
