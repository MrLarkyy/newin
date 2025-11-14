package lol.farsight.newin.registrar.asm.collector;

import lol.farsight.newin.annotation.method.Inject;
import lol.farsight.newin.registrar.asm.annotation.MethodSkeletonCollector;
import lol.farsight.newin.registrar.skeleton.MethodSkeleton;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

public sealed interface SkeletonCollector<T extends Annotation>
        permits InjectCollector
{
    SkeletonCollector<Inject> INJECT = new InjectCollector();

    @NotNull Class<T> annotationType();

    @NotNull MethodSkeleton apply(
            final @NotNull MethodSkeletonCollector collector,
            final @NotNull T annotation
    );
}