package app.recall.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stops a login attempt before the password is even checked when too many have failed.
 * The right password is refused too while locked, otherwise a guesser could keep going.
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    private final LoginThrottles throttles;
    private final Clock clock;

    public LoginThrottleFilter(LoginThrottles throttles, Clock clock) {
        this.throttles = throttles;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // The URI minus the context path, because getServletPath() is empty under MockMvc.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !("POST".equals(request.getMethod()) && "/login".equals(path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String email = request.getParameter("username");
        if (throttles.isBlocked(email == null ? "" : email, request.getRemoteAddr(), clock.instant())) {
            response.sendRedirect(request.getContextPath() + "/login?locked");
            return;
        }
        chain.doFilter(request, response);
    }
}
