package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG {@code place}: the transaction spans the whole handler body. MUST IGNORE {@code list}, which has none. */
@RestController
public class OrderController {

    @Transactional
    @PostMapping("/orders")
    public String place(@RequestBody String order) {
        return order.trim();
    }

    @GetMapping("/orders")
    public List<String> list() {
        return List.of("open");
    }
}
