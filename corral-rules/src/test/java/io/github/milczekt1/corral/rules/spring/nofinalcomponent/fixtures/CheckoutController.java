package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import org.springframework.stereotype.Controller;

/** MUST FLAG: a final {@code @Controller}. */
@Controller
public final class CheckoutController {

    public String checkout() {
        return "checkout";
    }
}
