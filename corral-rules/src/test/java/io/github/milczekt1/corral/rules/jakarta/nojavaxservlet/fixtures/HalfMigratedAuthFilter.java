package io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

/** MUST FLAG the {@code javax} annotation only: the {@code jakarta} interface beside it must not be reported. */
@javax.servlet.annotation.WebFilter("/*")
public class HalfMigratedAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(request, response);
    }
}
