package lol.farsight.newin.core.skeleton;

import com.google.common.base.Preconditions;
import lol.farsight.newin.core.asm.NewinClassVisitor;
import lol.farsight.newin.core.asm.inject.Injector;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

public record InjectSkeleton(
        @NotNull MethodSkeleton.Identifier identifier,
        @NotNull Injector injector
) implements MethodSkeleton {
    @Override
    public @NotNull MethodVisitor apply(
            final @NotNull NewinClassVisitor visitor,
            final @NotNull MethodVisitor mv,
            final int access,
            final @NotNull String name,
            final @NotNull String desc
    ) {
        Preconditions.checkNotNull(mv, "mv");

        return injector.apply(
                visitor.generator(),
                mv,
                access,
                name,
                desc
        );
    }
}
