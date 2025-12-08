package lol.farsight.newin.core.map;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public final class WeakKey<K> extends WeakReference<K> {
    private final int hash;

    public WeakKey(
            final K referent,
            final ReferenceQueue<K> queue
    ) {
        super(
                referent,
                queue
        );

        this.hash = System.identityHashCode(referent);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof WeakKey<?> other))
            return false;

        final Object t = this.get();
        final Object o = other.get();

        return t == o;
    }
}