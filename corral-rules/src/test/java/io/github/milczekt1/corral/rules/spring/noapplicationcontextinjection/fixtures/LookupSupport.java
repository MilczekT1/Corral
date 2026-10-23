package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/** MUST FLAG: the base class every subclass inherits the container from. */
public abstract class LookupSupport implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    protected ApplicationContext getApplicationContext() {
        return context;
    }

    protected <T> T bean(Class<T> type) {
        return context.getBean(type);
    }
}
