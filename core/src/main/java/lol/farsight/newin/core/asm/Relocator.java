package lol.farsight.newin.core.asm;

import com.google.common.base.Preconditions;
import lol.farsight.newin.core.transformer.BcTransformer;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.lang.invoke.MethodHandles;
import java.util.*;

public final class Relocator {
    private final Map<String, String> relocations = new HashMap<>();
    private final Collection<Class<?>> queue = new HashSet<>();

    private final @NotNull Class<?> target;

    public Relocator(final @NotNull Class<?> target) {
        Preconditions.checkNotNull(target, "target");

        this.target = target;
    }

    public Type add(final @NotNull Class<?> from) {
        Preconditions.checkNotNull(from, "from");

        final String newName = target.getPackageName().replace('.', '/')
                + "/"
                + from.getSimpleName()
                + "#"
                + UUID.randomUUID();

        queue.add(from);
        relocations.put(
                from.getName().replace('.', '/'),
                newName
        );

        return Type.getType(
                "L" + newName + ";"
        );
    }

    public void relocate() {
        for (final var from : queue) {
            BcTransformer.invoke(from, this::relocateClass);
        }
    }

    private byte @NotNull [] relocateClass(final byte @NotNull [] bytes) {
        final var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final var reader = new ClassReader(bytes);

        reader.accept(new ClassRemapper(
                writer,
                new SimpleRemapper(relocations)
        ), 0);

        final byte[] newBytes = writer.toByteArray();

        try {
            MethodHandles.privateLookupIn(
                    target,
                    MethodHandles.lookup()
            ).defineClass(newBytes);
        } catch (final @NotNull IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return bytes;
    }
}
