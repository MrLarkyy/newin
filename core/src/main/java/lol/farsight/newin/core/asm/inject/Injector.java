package lol.farsight.newin.core.asm.inject;

import com.google.common.base.Preconditions;
import lol.farsight.newin.core.annotation.method.Inject;
import lol.farsight.newin.core.asm.gen.EphemeralClassGenerator;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.concurrent.CompletableFuture;

public abstract sealed class Injector
        permits ExitInjector, HeadInjector {
    public static @NotNull Injector of(
            final int access,
            final @NotNull String name,
            final @NotNull String desc,
            final @NotNull String internalName,
            final @NotNull Inject inject
    ) {
        return switch (inject.at().point()) {
            case HEAD -> new HeadInjector(internalName, access, name, desc);
            case EXIT -> new ExitInjector(internalName, access, name, desc);
        };
    }

    protected final @NotNull String owner;
    protected final int access;
    protected final @NotNull String name;
    protected final @NotNull String desc;

    protected Injector(
            final @NotNull String owner,
            final int access,
            final @NotNull String name,
            final @NotNull String desc
    ) {
        Preconditions.checkNotNull(owner, "owner");
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(desc, "desc");

        this.owner = owner;
        this.access = access;
        this.name = name;
        this.desc = desc;
    }

    protected final void write(
            final @NotNull GeneratorAdapter mv,
            final @NotNull String name,
            final int access,
            final @NotNull String targetDesc
    ) {

        int index = 0;
        if ((access & Opcodes.ACC_STATIC) == 0) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);

            index++;
        }

        final var targetType = Type.getMethodType(targetDesc);

        for (final Type argument : targetType.getArgumentTypes()) {
            mv.visitVarInsn(
                    argument.getOpcode(Opcodes.ILOAD),
                    index
            );

            index += argument.getSize();
        }

        mv.visitTypeInsn(
                Opcodes.NEW,
                "java/util/concurrent/CompletableFuture"
        );

        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.DUP);

        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/util/concurrent/CompletableFuture",
                "<init>",
                "()V",
                false
        );

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                owner,
                name,
                desc,
                false
        );

        final var elseLabel = new Label();

        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/util/concurrent/CompletableFuture",
                "isDone",
                "()Z",
                false
        );

        mv.visitJumpInsn(
                Opcodes.IFEQ,
                elseLabel
        );

        final var ret = targetType.getReturnType();
        if (ret.getSort() == Type.VOID) {
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
        } else {
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/util/concurrent/CompletableFuture",
                    "join",
                    "()Ljava/lang/Object;",
                    false
            );

            mv.unbox(ret);

            mv.visitInsn(ret.getOpcode(Opcodes.IRETURN));
        }

        mv.visitLabel(elseLabel);
        mv.visitInsn(Opcodes.POP);
    }

    public abstract @NotNull MethodVisitor apply(
            final @NotNull EphemeralClassGenerator generator,
            final @NotNull MethodVisitor mv,
            final int access,
            final @NotNull String name,
            final @NotNull String desc
    );
}
