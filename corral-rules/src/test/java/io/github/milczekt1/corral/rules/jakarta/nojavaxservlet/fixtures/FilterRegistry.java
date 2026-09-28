package io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;

/** MUST FLAG: {@code javax} as a type argument only, the shape of a {@code FilterRegistrationBean<Filter>}. */
public class FilterRegistry {

    private final List<Filter> filters = new ArrayList<>();

    public int size() {
        return filters.size();
    }
}
