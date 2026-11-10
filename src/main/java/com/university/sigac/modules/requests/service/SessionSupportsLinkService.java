package com.university.sigac.modules.requests.service;

import com.university.sigac.core.exception.ApiException;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.repository.SessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the public ZIP download token in its own transaction so an SMTP failure
 * cannot roll it back after the email already went out with that link.
 */
@Service
@RequiredArgsConstructor
public class SessionSupportsLinkService {

    private final SessionRepository sessions;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String ensureToken(Long sessionId) {
        Session s = sessions.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (s.getSupportsDownloadToken() != null && !s.getSupportsDownloadToken().isBlank()) {
            return s.getSupportsDownloadToken();
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        s.setSupportsDownloadToken(token);
        sessions.save(s);
        return token;
    }
}
