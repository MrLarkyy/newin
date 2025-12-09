package lol.farsight.newin.core.asm;

import com.google.common.base.Preconditions;
import lol.farsight.newin.core.annotation.Newin;
import lol.farsight.newin.core.asm.annotation.AnnotationCollector;
import lol.farsight.newin.core.asm.annotation.MethodSkeletonCollector;
import lol.farsight.newin.core.asm.gen.EphemeralClassGenerator;
import lol.farsight.newin.core.map.IdentityWeakConcurrentMap;
import lol.farsight.newin.core.map.WeakKey;
import lol.farsight.newin.core.skeleton.MethodSkeleton;
import lol.farsight.newin.core.transformer.BcTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class NewinClassVisitor extends ClassVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewinClassVisitor.class);

    private final @NotNull Class<?> newinClass;

    private int version = -1;

    private EphemeralClassGenerator generator = null;
    private Newin newinAnnotation = null;
    private Type iwcmType = null;

    private final Map<MethodSkeleton.@NotNull Identifier, @NotNull MethodSkeleton> skeletons = new HashMap<>();

    public NewinClassVisitor(final @NotNull Class<?> newinClass) {
        super(Opcodes.ASM9);

        Preconditions.checkNotNull(newinClass, "newinClass");

        this.newinClass = newinClass;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final @NotNull String name,
            final @Nullable String signature,
            final @NotNull String superName,
            final @NotNull String @Nullable [] interfaces
    ) {
        this.version = version;
    }

    @Override
    public AnnotationVisitor visitAnnotation(
            final @NotNull String descriptor,
            final boolean visible
    ) {
        if (!Objects.equals(
                descriptor,
                Type.getDescriptor(Newin.class)
        )) return null;

        if (newinAnnotation != null)
            throw new RuntimeException("Found more than one @Newin annotation on class");

        return new AnnotationCollector(Newin.class) {
            @Override
            public void visitEnd() {
                newinAnnotation = (Newin) build();

                {
                    final var relocator = new Relocator(newinAnnotation.target());

                    iwcmType = relocator.add(IdentityWeakConcurrentMap.class);
                    relocator.add(WeakKey.class);

                    relocator.relocate();
                }

                generator = new EphemeralClassGenerator(
                        newinAnnotation.target(),
                        version,
                        iwcmType
                );
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final @NotNull String name,
            final @NotNull String desc,
            final @Nullable String signature,
            final @NotNull String @Nullable[] exceptions
    ) {
        return new MethodSkeletonCollector(
                newinClass.getName(),
                generator,
                skeletons,

                access,
                name,
                desc
        );
    }

    public void apply() {
        if (newinAnnotation == null
                || generator == null
                || iwcmType == null) {
            LOGGER.error("No @Newin annotation on class found");

            return;
        }

        final byte[] bytes = apply(
                BcTransformer.bytes(newinAnnotation.target())
        );

        BcTransformer.invoke(newinAnnotation.target(), b -> bytes);
    }

    private byte @NotNull [] apply(final byte @NotNull [] bytes) {
        final var classReader = new ClassReader(bytes);
        final var classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        classReader.accept(new ClassVisitor(Opcodes.ASM9, classWriter) {
            @Override
            public MethodVisitor visitMethod(
                    final int access,
                    final @NotNull String name,
                    final @NotNull String desc,
                    final @Nullable String signature,
                    final String @Nullable [] exceptions
            ) {
                final var mv = super.visitMethod(access, name, desc, signature, exceptions);
                final var skeleton = skeletons.get(
                        new MethodSkeleton.Identifier(
                                name,
                                Type.getMethodType(desc)
                                        .getArgumentTypes()
                        )
                );

                if (skeleton != null)
                    return skeleton.apply(
                            NewinClassVisitor.this,
                            mv,
                            access,
                            name,
                            desc
                    );

                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);

        try {
            final var cl = generator.define();
            final var ctor = MethodHandles.lookup()
                    .unreflectConstructor(newinClass.getDeclaredConstructor());

            cl.getMethod("factory", Supplier.class)
                    .invoke(null, (Supplier<Object>) () -> {
                        try {
                            return ctor.invoke();
                        } catch (final Throwable e) {
                            throw new RuntimeException(e);
                        }
                    });

            cl.getMethod("type", Class.class)
                    .invoke(null, newinClass);
        } catch (final @NotNull Exception e) {
            LOGGER.error("Failed to initialize ephemeral class, are you sure your newin class is public and your constructor has no parameters and is public?", e);

            return bytes;
        }

        return classWriter.toByteArray();
    }

    public EphemeralClassGenerator generator() {
        return generator;
    }
}
