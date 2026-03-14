package be.vdab.tcoaching.api.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
    public String resolve(HttpServletRequest request) {
        // When ForwardedHeaderFilter is enabled, Spring mutates the request so
        // getRemoteAddr() already reflects the trusted forwarded client IP.
        return request.getRemoteAddr();
    }
}
