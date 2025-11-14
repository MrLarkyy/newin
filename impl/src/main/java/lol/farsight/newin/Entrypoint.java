package lol.farsight.newin;

import lol.farsight.newin.registrar.Newins;
import me.lucko.spark.paper.lib.protobuf.Mixin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
