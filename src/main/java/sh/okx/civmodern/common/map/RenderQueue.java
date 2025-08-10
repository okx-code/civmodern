package sh.okx.civmodern.common.map;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RenderQueue {
    private static final Deque<Runnable> runnables = new ConcurrentLinkedDeque<>();
    public static void queue(Runnable r) {
        runnables.add(r);
    }

    public static void runAll() {
        Runnable r;
        while ((r = runnables.poll()) != null) {
            r.run();
        }
    }
}
