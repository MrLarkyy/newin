package lol.farsight.newin;

import lol.farsight.newin.api.NewinManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class Entrypoint extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer()
                .getServicesManager()
                .register(
                        NewinManager.class,
                        Newins.INSTANCE,
                        this,
                        ServicePriority.Normal
                );
    }
}
