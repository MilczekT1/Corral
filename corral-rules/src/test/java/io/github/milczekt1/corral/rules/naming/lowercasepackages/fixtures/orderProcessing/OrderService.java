package io.github.milczekt1.corral.rules.naming.lowercasepackages.fixtures.orderProcessing;

import java.util.ArrayList;
import java.util.List;

/** MUST FLAG once: a camelCase segment. Its nested and anonymous classes are not reported separately. */
public class OrderService {

    private final List<Line> lines = new ArrayList<>();

    public void add(String sku, int quantity) {
        lines.add(new Line(sku, quantity));
    }

    public Runnable clearer() {
        return new Runnable() {
            @Override
            public void run() {
                lines.clear();
            }
        };
    }

    static class Line {

        private final String sku;
        private final int quantity;

        Line(String sku, int quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }

        String describe() {
            return quantity + " x " + sku;
        }
    }
}
