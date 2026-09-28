package io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/** MUST FLAG: a {@code javax.servlet.http} parameter, and a declared {@code javax} {@code ServletException}. */
public class LegacyRequestLogger {

    public String describe(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }

    public void requireTenant(String tenant) throws ServletException {
        if (tenant == null) {
            throw new ServletException("missing tenant");
        }
    }
}
