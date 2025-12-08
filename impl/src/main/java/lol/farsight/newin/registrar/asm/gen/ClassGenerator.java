package lol.farsight.newin.registrar.asm.gen;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

public sealed abstract class ClassGenerator
        permits EphemeralClassGenerator
{
    protected final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    protected final @NotNull Class<?> target;
    public final @NotNull String internalName;

    protected ClassGenerator(
            final @NotNull Class<?> target,
            final int version,
            final @NotNull String internalName
    ) {
        Preconditions.checkNotNull(target, "target");
        Preconditions.checkNotNull(internalName, "internalName");

        this.target = target;
        this.internalName = internalName;

        writer.visit(
                version,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                internalName,
                null,
                "java/lang/Object",
                null
        );
    }

    protected final void field(
            final int access,
            final @NotNull String name,
            final @NotNull String descriptor,
            final @Nullable String signature,
            final @Nullable Object value,
            final @NotNull Consumer<? super FieldVisitor> consumer
    ) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(descriptor, "descriptor");
        Preconditions.checkNotNull(consumer, "consumer");

        final var fv = writer.visitField(
                access,
                name,
                descriptor,
                signature,
                value
        );

        consumer.accept(fv);

        fv.visitEnd();
    }

    protected final void field(
            final int access,
            final @NotNull String name,
            final @NotNull String descriptor,
            final @Nullable String signature,
            final @Nullable Object value
    ) {
        field(
                access,
                name,
                descriptor,
                signature,
                value,
                fv -> {}
        );
    }

    protected final void method(
            final int access,
            final @NotNull String name,
            final @NotNull String desc,
            final @Nullable String signature,
            final @NotNull String @Nullable [] exceptions,
            final @NotNull Consumer<? super MethodVisitor> consumer
    ) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(desc, "desc");
        Preconditions.checkNotNull(consumer, "consumer");

        final var mv = writer.visitMethod(
                access,
                name,
                desc,
                signature,
                exceptions
        );

        mv.visitCode();

        consumer.accept(mv);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public final Class<?> define() {
        writer.visitEnd();

        final byte[] bytes = writer.toByteArray();
        try {
            return MethodHandles.privateLookupIn(
                    target,
                    MethodHandles.lookup()
            ).defineClass(bytes);
        } catch (final @NotNull IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
