package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/** MUST FLAG: the container arrives through the {@code Aware} callback instead of the constructor. */
@Component
public class ContextAwareRegistry implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public int size() {
        return context.getBeanDefinitionCount();
    }
}
