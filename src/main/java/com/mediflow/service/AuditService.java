package com.mediflow.service;

import com.mediflow.entity.AuditLog;
import com.mediflow.entity.User;
import com.mediflow.repository.AuditLogRepository;
import com.mediflow.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /** Records an action performed by the currently authenticated user. */
    public void log(String action, String entityType, String entityId, String details) {
        try {
            User user = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null
                    && !"anonymousUser".equals(auth.getName())) {
                Optional<User> u = userRepository.findByEmailIgnoreCase(auth.getName());
                if (u.isPresent()) user = u.get();
            }
            AuditLog entry = new AuditLog();
            entry.setUser(user);
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDetails(details == null ? null : details.substring(0, Math.min(details.length(), 500)));
            auditLogRepository.save(entry);
        } catch (Exception ignored) {
            // Auditing must never break the business action
        }
    }
}
