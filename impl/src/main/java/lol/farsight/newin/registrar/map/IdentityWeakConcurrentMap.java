package lol.farsight.newin.registrar.map;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class IdentityWeakConcurrentMap<K> {
    private Supplier<?> factory = () -> null;

    private final ConcurrentHashMap<WeakKey<K>, Object> map = new ConcurrentHashMap<>();
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    private void cleanup() {
        Reference<? extends K> ref;
        while ((ref = queue.poll()) != null)
            map.remove(ref);
    }

    public @NotNull Object compute(final @NotNull K key) {
        cleanup();

        return map.computeIfAbsent(
                new WeakKey<>(key, queue),
                wk -> Preconditions.checkNotNull(factory.get(), "factory.get()")
        );
    }

    public void factory(final @NotNull Supplier<? extends @NotNull Object> factory) {
        Preconditions.checkNotNull(factory, "factory");

        this.factory = factory;
    }
}
