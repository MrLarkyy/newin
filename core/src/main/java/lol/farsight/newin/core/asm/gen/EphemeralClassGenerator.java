package lol.farsight.newin.core.asm.gen;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class EphemeralClassGenerator extends ClassGenerator {
    public static final String INSTANCES_FIELD_NAME = "#INSTANCES#";
    public static final String TYPE_FIELD_NAME = "#TYPE#";

    private final @NotNull Type iwcm;

    public EphemeralClassGenerator(
            final @NotNull Class<?> target,
            final int version,
            final @NotNull Type iwcm
    ) {
        super(
                target,
                version,
                target.getPackageName().replace('.', '/')
                        + "/"
                        + target.getSimpleName()
                        + "#"
                        + UUID.randomUUID()
        );

        Preconditions.checkNotNull(iwcm, "iwcm");

        this.iwcm = iwcm;

        field(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                INSTANCES_FIELD_NAME,
                iwcm.getDescriptor(),
                null,
                null
        );

        field(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                TYPE_FIELD_NAME,
                "Ljava/lang/Class;",
                null,
                null
        );

        method(
                Opcodes.ACC_PRIVATE,
                "<init>",
                "()V",
                null,
                null,
                this::initializeInit
        );

        method(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null,
                this::initializeClinit
        );

        method(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "factory",
                "(Ljava/util/function/Supplier;)V",
                null,
                null,
                this::initializeFactory
        );

        method(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "type",
                "(Ljava/lang/Class;)V",
                null,
                null,
                this::initializeType
        );
    }

    public @NotNull String injection(
            final int access,
            final @NotNull String name,
            final @NotNull String desc
    ) {
        final String newName = name + "#" + UUID.randomUUID();

        method(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                newName,
                desc,
                null,
                null,
                mv -> {
                    if ((access & Opcodes.ACC_STATIC) == 0)
                        initializeInjection(mv, name, desc);
                    else initializeStaticInjection(mv, name, desc);
                }
        );

        return newName;
    }

    private void initializeStaticInjection(final @NotNull MethodVisitor mv, final @NotNull String name, final @NotNull String descriptor) {
        final var start = new Label();
        final var handler = new Label();
        final var ret = new Label();

        final var methodType = Type.getMethodType(descriptor);

        mv.visitTryCatchBlock(start, ret, handler, "java/lang/Throwable");

        mv.visitLabel(start);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
        mv.visitFieldInsn(Opcodes.GETSTATIC, internalName, TYPE_FIELD_NAME, "Ljava/lang/Class;");

        mv.visitLdcInsn(name);

        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");

        mv.visitLdcInsn(methodType.getArgumentCount());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");

        final Type[] argumentTypes = methodType.getArgumentTypes();
        for (int i = 0; i < argumentTypes.length; i++) {
            final var argument = argumentTypes[i];

            mv.visitInsn(Opcodes.DUP);

            mv.visitLdcInsn(i);
            mv.visitLdcInsn(argument);
            mv.visitInsn(Opcodes.AASTORE);
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);

        int index = 0;
        for (final Type argument : methodType.getArgumentTypes()) {
            mv.visitVarInsn(
                    argument.getOpcode(Opcodes.ILOAD),
                    index
            );

            index += argument.getSize();
        }

        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                "invoke",
                "(" + "Ljava/lang/Object;".repeat(methodType.getArgumentCount()) + ")V",
                false
        );

        mv.visitJumpInsn(Opcodes.GOTO, ret);

        mv.visitLabel(handler);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);

        mv.visitLabel(ret);
        mv.visitInsn(Opcodes.RETURN);
    }

    private void initializeInjection(final @NotNull MethodVisitor mv, final @NotNull String name, final @NotNull String descriptor) {
        final var start = new Label();
        final var handler = new Label();
        final var ret = new Label();

        final var methodType = Type.getMethodType(descriptor);

        int index = 0;
        for (final var argument : methodType.getArgumentTypes()) {
            index += argument.getSize();
        }

        mv.visitTryCatchBlock(start, ret, handler, "java/lang/Throwable");

        mv.visitFieldInsn(Opcodes.GETSTATIC, internalName, INSTANCES_FIELD_NAME, iwcm.getDescriptor());
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, iwcm.getInternalName(), "compute", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitVarInsn(Opcodes.ASTORE, index);

        mv.visitLabel(start);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
        mv.visitVarInsn(Opcodes.ALOAD, index);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);

        mv.visitLdcInsn(name);

        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");

        mv.visitLdcInsn(methodType.getArgumentCount());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");

        final Type[] argumentTypes = methodType.getArgumentTypes();
        for (int i = 0; i < argumentTypes.length; i++) {
            final var argument = argumentTypes[i];

            mv.visitInsn(Opcodes.DUP);

            mv.visitLdcInsn(i);
            mv.visitLdcInsn(argument);
            mv.visitInsn(Opcodes.AASTORE);
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(Opcodes.ALOAD, index);

        index = 0;
        for (final var argument : methodType.getArgumentTypes()) {
            mv.visitVarInsn(
                    argument.getOpcode(Opcodes.ILOAD),
                    index
            );

            index += argument.getSize();
        }

        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                "invoke",
                "(" + "Ljava/lang/Object;".repeat(methodType.getArgumentCount() + 1) + ")V",
                false
        );

        mv.visitJumpInsn(Opcodes.GOTO, ret);

        mv.visitLabel(handler);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);

        mv.visitLabel(ret);
        mv.visitInsn(Opcodes.RETURN);
    }

    private void initializeInit(final @NotNull MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
        );

        mv.visitTypeInsn(
                Opcodes.NEW,
                "java/lang/UnsupportedOperationException"
        );

        mv.visitInsn(Opcodes.DUP);

        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/UnsupportedOperationException",
                "<init>",
                "()V",
                false
        );

        mv.visitInsn(Opcodes.ATHROW);
    }

    private void initializeClinit(final @NotNull MethodVisitor mv) {
        mv.visitTypeInsn(
                Opcodes.NEW,
                iwcm.getInternalName()
        );

        mv.visitInsn(Opcodes.DUP);

        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                iwcm.getInternalName(),
                "<init>",
                "()V",
                false
        );

        mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                internalName,
                INSTANCES_FIELD_NAME,
                iwcm.getDescriptor()
        );

        mv.visitInsn(Opcodes.RETURN);
    }

    private void initializeFactory(final @NotNull MethodVisitor mv) {
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                internalName,
                INSTANCES_FIELD_NAME,
                iwcm.getDescriptor()
        );

        mv.visitVarInsn(
                Opcodes.ALOAD,
                0
        );

        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                iwcm.getInternalName(),
                "factory",
                "(Ljava/util/function/Supplier;)V",
                false
        );

        mv.visitInsn(Opcodes.RETURN);
    }

    private void initializeType(final @NotNull MethodVisitor mv) {
        mv.visitVarInsn(
                Opcodes.ALOAD,
                0
        );

        mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                internalName,
                TYPE_FIELD_NAME,
                "Ljava/lang/Class;"
        );

        mv.visitInsn(Opcodes.RETURN);
    }
}
