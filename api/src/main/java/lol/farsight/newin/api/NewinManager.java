package lol.farsight.newin.api;

import org.jetbrains.annotations.NotNull;

public interface NewinManager {
    void applyFromPackage(
            final @NotNull Object pluginObj,
            final @NotNull String pack
    );

    void applyFromClasses(final @NotNull Class<?> @NotNull ... classes);
}
