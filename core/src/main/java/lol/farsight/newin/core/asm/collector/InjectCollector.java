package lol.farsight.newin.core.asm.collector;

import lol.farsight.newin.core.annotation.method.Inject;
import lol.farsight.newin.core.asm.annotation.MethodSkeletonCollector;
import lol.farsight.newin.core.asm.inject.Injector;
import lol.farsight.newin.core.skeleton.InjectSkeleton;
import lol.farsight.newin.core.skeleton.MethodSkeleton;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

final class InjectCollector implements SkeletonCollector<Inject> {
    InjectCollector() { }

    @Override
    public @NotNull Class<Inject> annotationType() {
        return Inject.class;
    }

    @Override
    public @NotNull MethodSkeleton apply(
            final @NotNull MethodSkeletonCollector collector,
            final @NotNull Inject annotation
    ) {
        final Type methodType = Type.getMethodType(collector.desc);

        final Type[] arguments = Arrays.copyOfRange(
                methodType.getArgumentTypes(),
                (collector.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0,
                methodType.getArgumentCount() - 1
        );

        return new InjectSkeleton(
                new MethodSkeleton.Identifier(
                        annotation.name(),
                        arguments
                ),

                Injector.of(
                        collector.access,
                        collector.name,
                        collector.desc,
                        collector.generator.internalName,
                        annotation
                )
        );
    }
}
