package lol.farsight.newin.inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TypedReturnRedirect<T> {
    private final Class<?> type;

    private boolean redirected = false;
    private @Nullable T val = null;

    public TypedReturnRedirect(final @NotNull Class<?> type) {
        if (type == null)
            throw new NullPointerException("type");

        this.type = type;
    }

    public void redirect(final @Nullable T value) {
        if (val != null && !val.getClass().isAssignableFrom(type))
            throw new IllegalArgumentException("unexpected type");

        redirected = true;
        val = value;
    }

    public @Nullable T val() {
        return val;
    }

    public boolean redirected() {
        return redirected;
    }
}
