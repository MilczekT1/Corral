package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

import java.util.concurrent.RecursiveTask;

/** MUST IGNORE: {@code ForkJoinTask.join()} inside {@code compute()}, which has no timeout overload. */
public class TileSum extends RecursiveTask<Long> {

    private final long[] tiles;

    private final int from;

    private final int to;

    public TileSum(long[] tiles, int from, int to) {
        this.tiles = tiles;
        this.from = from;
        this.to = to;
    }

    @Override
    protected Long compute() {
        if (to - from <= 2) {
            long sum = 0;
            for (int i = from; i < to; i++) {
                sum += tiles[i];
            }
            return sum;
        }
        int middle = (from + to) >>> 1;
        TileSum left = new TileSum(tiles, from, middle);
        left.fork();
        return new TileSum(tiles, middle, to).compute() + left.join();
    }
}
