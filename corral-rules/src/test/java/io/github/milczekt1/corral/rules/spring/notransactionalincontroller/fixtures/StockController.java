package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG the private helper: inert behind the proxy, but still a transaction declared in a controller. */
@RestController
public class StockController {

    private int reserved;

    @PostMapping("/stock")
    public int reserveOne() {
        reserve();
        return reserved;
    }

    @Transactional
    private void reserve() {
        reserved++;
    }
}
