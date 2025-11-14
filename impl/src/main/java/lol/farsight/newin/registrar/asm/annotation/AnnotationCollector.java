package lol.farsight.newin.registrar.asm.annotation;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.*;

public class AnnotationCollector extends AnnotationVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationCollector.class);

    private final @NotNull Class<? extends Annotation> annotationClass;
    private final Map<String, Object> values = new HashMap<>();

    public AnnotationCollector(final @NotNull Class<? extends Annotation> annotationClass) {
        super(Opcodes.ASM9);

        Preconditions.checkNotNull(annotationClass, "annotationClass");

        this.annotationClass = annotationClass;
    }

    @Override
    public void visit(
            final @NotNull String name,
            final @NotNull Object value
    ) {
        if (value instanceof final @NotNull Type type) {
            if (type.getSort() != Type.OBJECT)
                return;

            final Class<?> objectType;
            try {
                objectType = annotationClass.getClassLoader()
                        .loadClass(type.getClassName());
            } catch (final @NotNull ClassNotFoundException e) {
                return;
            }

            values.put(name, objectType);

            return;
        }

        values.put(name, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void visitEnum(
            final @NotNull String name,
            final @NotNull String desc,
            final @NotNull String value
    ) {
        final Class<? extends Enum> enumType;
        try {
            enumType = (Class<? extends Enum>) Class.forName(
                    Type.getType(desc).getClassName(),
                    true,
                    annotationClass.getClassLoader()
            );
        } catch (final @NotNull ClassNotFoundException e) {
            LOGGER.error("Couldn't find class of enum", e);

            return;
        }

        values.put(
                name,
                Enum.valueOf(enumType, value)
        );
    }

    @Override
    public AnnotationVisitor visitArray(final @NotNull String name) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            final List<Object> list = new ArrayList<>();

            @Override
            public void visit(
                    final @NotNull String name,
                    final @NotNull Object value
            ) {
                if (value instanceof final @NotNull Type type) {
                    if (type.getSort() != Type.OBJECT)
                        return;

                    final Class<?> objectType;
                    try {
                        objectType = Class.forName(
                                type.getClassName(),
                                true,
                                annotationClass.getClassLoader()
                        );
                    } catch (final @NotNull ClassNotFoundException e) {
                        LOGGER.error("Couldn't find class of object", e);

                        return;
                    }

                    list.add(objectType);

                    return;
                }

                list.add(value);
            }

            @Override
            public void visitEnd() {
                values.put(name, list.toArray());
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public AnnotationVisitor visitAnnotation(
            final @NotNull String name,
            final @NotNull String descriptor
    ) {
        final Class<? extends Annotation> annotationType;
        try {
            annotationType = (Class<? extends Annotation>) Class.forName(
                    Type.getType(descriptor).getClassName(),
                    true,
                    annotationClass.getClassLoader()
            );
        } catch (final @NotNull ClassNotFoundException e) {
            LOGGER.error("Couldn't find class of annotation", e);

            return null;
        }

        return new AnnotationCollector(annotationType) {
            @Override
            public void visitEnd() {
                values.put(name, build());
            }
        };
    }

    public @NotNull Annotation build() {
        return (Annotation) Proxy.newProxyInstance(
                annotationClass.getClassLoader(),
                new Class<?>[] { annotationClass },
                (proxy, method, args) -> {
                    if (method.getName().equals("annotationType"))
                        return annotationClass;

                    final var ret = values.get(method.getName());
                    if (method.getReturnType().isArray() && ret.getClass().isArray()) {
                        final int length = ((Object[]) ret).length;

                        final var result = Array.newInstance(method.getReturnType().componentType(), length);
                        System.arraycopy(ret, 0, result, 0, length);

                        return result;
                    }

                    return ret;
                }
        );
    }
}