package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: implements an interface method annotated {@code @Transactional}, though its own override is not. */
@RestController
public class ArchiveController implements ArchiveOperations {

    private int purged;

    @Override
    public int purge() {
        return ++purged;
    }
}
