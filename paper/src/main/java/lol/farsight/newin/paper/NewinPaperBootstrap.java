package lol.farsight.newin.paper;

import com.google.common.base.Preconditions;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import lol.farsight.newin.core.agent.AgentEntrypoint;
import lol.farsight.newin.core.agent.InstrumentationHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class NewinPaperBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewinPaperBootstrap.class);

    private static final Method GET_FILE_METHOD;
    static {
        try {
            GET_FILE_METHOD = JavaPlugin.class.getDeclaredMethod("getFile");
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        GET_FILE_METHOD.setAccessible(true);
    }

    private NewinPaperBootstrap() {}

    public static void acknowledge(final @NotNull JavaPlugin plugin) {
        Preconditions.checkNotNull(plugin, "plugin");

        final Path agent;
        {
            final var dataFolder = plugin.getDataFolder();
            dataFolder.mkdirs();

            agent = dataFolder.toPath()
                    .resolve(Path.of("agent.jar"));

            final var manifest = new Manifest();
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            manifest.getMainAttributes().putValue("Agent-Class", AgentEntrypoint.class.getName());
            manifest.getMainAttributes().putValue("Can-Redefine-Classes", "true");
            manifest.getMainAttributes().putValue("Can-Retransform-Classes", "true");

            final File pluginJar;
            try {
                pluginJar = (File) GET_FILE_METHOD.invoke(plugin);
            } catch (final InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            try (
                    final var jar = new JarFile(pluginJar);
                    final var jos = new JarOutputStream(
                            Files.newOutputStream(agent),
                            manifest
                    )
            ) {
                String name;
                JarEntry entry;
                {
                    name = AgentEntrypoint.class.getName().replace('.', '/') + ".class";
                    entry = jar.getJarEntry(name);

                    jos.putNextEntry(entry);
                    try (final var in = jar.getInputStream(entry)) {
                        in.transferTo(jos);
                    }

                    jos.closeEntry();
                }

                {
                    name = InstrumentationHolder.class.getName().replace('.', '/') + ".class";
                    entry = jar.getJarEntry(name);

                    jos.putNextEntry(entry);
                    try (final var in = jar.getInputStream(entry)) {
                        in.transferTo(jos);
                    }

                    jos.closeEntry();
                }
            } catch (final @NotNull IOException e) {
                LOGGER.error("couldn't generate agent JAR");

                return;
            }
        }

        final Unsafe unsafe;
        try {
            final var theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);

            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (final @NotNull NoSuchFieldException | @NotNull IllegalAccessException e) {
            LOGGER.error("couldn't get unsafe instance", e);

            return;
        }

        try {
            final var allowSelfAttach = Class.forName("sun.tools.attach.HotSpotVirtualMachine")
                    .getDeclaredField("ALLOW_ATTACH_SELF");

            // noinspection removal
            unsafe.putBooleanVolatile(
                    unsafe.staticFieldBase(allowSelfAttach),
                    unsafe.staticFieldOffset(allowSelfAttach),
                    true
            );
        } catch (final @NotNull NoSuchFieldException | @NotNull ClassNotFoundException e) {
            LOGGER.error("couldn't force ALLOW_ATTACH_SELF", e);

            return;
        }

        try {
            final String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

            final var virtualMachine = VirtualMachine.attach(pid);
            virtualMachine.loadAgent(agent.toString(), "");
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
