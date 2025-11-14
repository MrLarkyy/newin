package lol.farsight.newin.registrar.asm.annotation;

import com.google.common.base.Preconditions;
import lol.farsight.newin.registrar.asm.collector.SkeletonCollector;
import lol.farsight.newin.registrar.asm.gen.EphemeralClassGenerator;
import lol.farsight.newin.registrar.skeleton.MethodSkeleton;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.*;

public final class MethodSkeletonCollector extends MethodVisitor {
    private static final Map<@NotNull String, @NotNull SkeletonCollector<? super Annotation>> COLLECTORS = new HashMap<>();

    static {
        skeleton(SkeletonCollector.INJECT);
    }

    @SuppressWarnings("unchecked")
    private static void skeleton(final @NotNull SkeletonCollector<?> collector) {
        COLLECTORS.put(
                Type.getDescriptor(
                        collector.annotationType()
                ),
                (SkeletonCollector<? super Annotation>) collector
        );
    }

    public final int access;
    public final @NotNull String name;
    public final @NotNull String desc;

    private final @NotNull String newinName;
    public final @NotNull EphemeralClassGenerator generator;
    private final @NotNull Map<MethodSkeleton.Identifier, MethodSkeleton> skeletons;

    private AnnotationCollector annotationCollector = null;

    private final Set<NewinError> errors = new HashSet<>();

    public MethodSkeletonCollector(
            final @NotNull String newinName,
            final @NotNull EphemeralClassGenerator generator,
            final @NotNull Map<MethodSkeleton.Identifier, MethodSkeleton> skeletons,

            final int access,
            final @NotNull String name,
            final @NotNull String desc
    ) {
        super(Opcodes.ASM9);

        Preconditions.checkNotNull(newinName, "newinName");
        Preconditions.checkNotNull(generator, "generator");
        Preconditions.checkNotNull(skeletons, "skeletons");

        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(desc, "desc");

        this.newinName = newinName;
        this.generator = generator;
        this.skeletons = skeletons;

        this.access = access;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public AnnotationVisitor visitAnnotation(
            final @NotNull String descriptor,
            final boolean visible
    ) {
        final var collector = COLLECTORS.get(descriptor);
        if (collector != null) {
            if (annotationCollector != null) {
                errors.add(NewinError.MULTIPLE_ANNOTATIONS);

                return null;
            }

            return annotationCollector = new AnnotationCollector(collector.annotationType());
        }

        return null;
    }

    @Override
    public void visitEnd() {
        if (annotationCollector == null)
            errors.add(NewinError.NO_ANNOTATIONS);

        if (!errors.isEmpty()) {
            for (final var error : errors) {
                error.log(newinName, name);
            }

            return;
        }

        final var annotation = annotationCollector.build();
        final var mapper = COLLECTORS.get(
                Type.getDescriptor(annotation.annotationType())
        );

        final var result = mapper.apply(
                this,
                annotation
        );

        final var skeleton = Objects.requireNonNull(result);
        skeletons.put(
                skeleton.identifier(),
                skeleton
        );
    }
}
