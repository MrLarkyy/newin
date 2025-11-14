package lol.farsight.newin.registrar.skeleton;

import lol.farsight.newin.registrar.asm.NewinClassVisitor;
import lol.farsight.newin.registrar.asm.gen.EphemeralClassGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Objects;

public sealed interface MethodSkeleton
        permits InjectSkeleton
{
    record Identifier(@NotNull String name, Type @NotNull [] arguments) {
            public Identifier(
                    @NotNull String name,
                    @NotNull Type @NotNull [] arguments
            ) {
                this.name = name;
                this.arguments = arguments;
            }

            @Override
            public boolean equals(final @Nullable Object obj) {
                if (obj == this)
                    return true;
                if (obj == null || obj.getClass() != this.getClass())
                    return false;

                final var that = (Identifier) obj;
                return Objects.equals(this.name, that.name) &&
                        Arrays.equals(this.arguments, that.arguments);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, Arrays.hashCode(arguments));
            }
    }

    @NotNull MethodSkeleton.Identifier identifier();

    @NotNull MethodVisitor apply(
            final @NotNull NewinClassVisitor visitor,
            final @NotNull MethodVisitor mv,
            final int access,
            final @NotNull String name,
            final @NotNull String desc
    );
}
