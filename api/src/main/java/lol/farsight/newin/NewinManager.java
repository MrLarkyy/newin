package lol.farsight.newin;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public interface NewinManager {
    void applyToPackage(
            final @NotNull JavaPlugin plugin,
            final @NotNull String pack
    );

    void applyToClasses(final @NotNull Class<?> @NotNull ... classes);
}
