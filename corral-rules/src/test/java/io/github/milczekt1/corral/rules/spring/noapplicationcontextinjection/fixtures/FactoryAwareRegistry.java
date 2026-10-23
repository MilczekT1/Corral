package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

/** MUST FLAG: {@code BeanFactory} itself, through its {@code Aware} callback. */
@Component
public class FactoryAwareRegistry implements BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public boolean defines(String name) {
        return beanFactory.containsBean(name);
    }
}
