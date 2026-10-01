package com.universidad.sigac.security;

import com.universidad.sigac.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Resuelve la IP de origen. Solo confía en X-Forwarded-For si sigac.trust-proxy-headers=true. */
@Component
public class ClientIpResolver {

    private final boolean trustProxy;

    public ClientIpResolver(AppProperties props) {
        this.trustProxy = props.isTrustProxyHeaders();
    }

    public String current() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return resolve(sra.getRequest());
        }
        return "N/A";
    }

    public String resolve(HttpServletRequest req) {
        if (trustProxy) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return req.getRemoteAddr();
    }

    public String currentUri() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest r = sra.getRequest();
            return r.getMethod() + " " + r.getRequestURI();
        }
        return "";
    }
}
