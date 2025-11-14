package lol.farsight.newin.registrar.asm.collector;

import lol.farsight.newin.annotation.method.Inject;
import lol.farsight.newin.registrar.asm.annotation.MethodSkeletonCollector;
import lol.farsight.newin.registrar.asm.inject.Injector;
import lol.farsight.newin.registrar.skeleton.InjectSkeleton;
import lol.farsight.newin.registrar.skeleton.MethodSkeleton;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;

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

        final Type[] arguments;
        if ((collector.access & Opcodes.ACC_STATIC) == 0)
            arguments = Arrays.copyOfRange(
                    methodType.getArgumentTypes(),
                    1,
                    methodType.getArgumentCount()
            );
        else arguments = methodType.getArgumentTypes();

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
