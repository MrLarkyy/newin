package lol.farsight.newin.core.transformer;

import com.google.common.base.Preconditions;
import lol.farsight.newin.agent.InstrumentationHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@SuppressWarnings("ClassCanBeRecord")
public final class BcTransformer implements ClassFileTransformer {
    public static void invoke(
            final @NotNull Class<?> cl,
            final @NotNull Function<byte @NotNull [], byte @NotNull []> factory
    ) {
        final var instrumentation = InstrumentationHolder.get();
        final var bcf = new BcTransformer(
                instrumentation,
                cl,
                factory
        );

        // setting canRetransform to true basically means
        // that we are allowing the instrumentation to run
        // the transformer when requested by #retransformClasses
        instrumentation.addTransformer(bcf, true);

        try {
            instrumentation.retransformClasses(cl);
        } catch (final @NotNull UnmodifiableClassException e) {
            // unreachable
            throw new UnsupportedOperationException();
        }
    }

    public static byte @NotNull [] bytes(final @NotNull Class<?> cl) {
        final AtomicReference<byte[]> bytes = new AtomicReference<>();
        invoke(cl, buffer -> {
            bytes.set(buffer);

            return buffer;
        });

        if (bytes.get() == null)
            throw new RuntimeException("couldn't fetch bytecode of class " + cl);

        return bytes.get();
    }

    private final @NotNull Instrumentation instrumentation;
    private final @NotNull Class<?> cl;
    private final @NotNull Function<byte @NotNull [], byte @NotNull []> factory;

    private BcTransformer(
            final @NotNull Instrumentation instrumentation,
            final @NotNull Class<?> cl,
            final @NotNull Function<byte @NotNull [], byte @NotNull []> factory
    ) {
        Preconditions.checkNotNull(instrumentation, "instrumentation");
        Preconditions.checkNotNull(cl, "cl");
        Preconditions.checkNotNull(factory, "factory");

        this.instrumentation = instrumentation;
        this.cl = cl;
        this.factory = factory;
    }

    @Override
    public byte @Nullable [] transform(
            final @NotNull ClassLoader loader,
            final @NotNull String className,
            final @NotNull Class<?> classBeingRedefined,
            final @NotNull ProtectionDomain protectionDomain,
            final byte @NotNull [] classfileBuffer
    ) {
        if (cl == classBeingRedefined) {
            try {
                return factory.apply(classfileBuffer);
            } finally {
                instrumentation.removeTransformer(this);
            }
        }

        return classfileBuffer;
    }
}
