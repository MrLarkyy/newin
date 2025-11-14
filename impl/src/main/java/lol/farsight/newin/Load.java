package lol.farsight.newin;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import lol.farsight.newin.agent.InstrumentationHolder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.management.ManagementFactory;

// earliest point a paper plugin can
// possibly run code at

@SuppressWarnings("UnstableApiUsage")
public final class Load implements PluginLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Load.class);

    @SuppressWarnings("removal")
    @Override
    public void classloader(final @NotNull PluginClasspathBuilder builder) {
        final Unsafe unsafe;
        try {
            final var theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);

            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (final @NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
            LOGGER.error("Couldn't get unsafe instance", e);

            return;
        }

        try {
            final var allowSelfAttach = Class.forName("sun.tools.attach.HotSpotVirtualMachine")
                    .getDeclaredField("ALLOW_ATTACH_SELF");

            // noinspection removal
            unsafe.putBoolean(
                    unsafe.staticFieldBase(allowSelfAttach),
                    unsafe.staticFieldOffset(allowSelfAttach),
                    true
            );
        } catch (final @NotNull NoSuchFieldException | @NotNull ClassNotFoundException e) {
            LOGGER.error("Couldn't force ALLOW_ATTACH_SELF", e);

            return;
        }

        try {
            final String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

            final var virtualMachine = VirtualMachine.attach(pid);
            virtualMachine.loadAgent(builder.getContext().getPluginSource().toAbsolutePath().toString(), "");
            virtualMachine.detach();
        } catch (final @NotNull AttachNotSupportedException | @NotNull IOException e) {
            LOGGER.error("couldn't attach to running JVM", e);

            return;
        } catch (final @NotNull AgentLoadException | @NotNull AgentInitializationException e) {
            LOGGER.error("couldn't attach agent to self", e);

            return;
        }

        // we call get() but we dont actually use the return value
        // this is purely here to cache the instrumentation value
        InstrumentationHolder.get();
    }
}
