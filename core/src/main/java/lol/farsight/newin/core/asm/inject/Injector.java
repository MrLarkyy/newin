package lol.farsight.newin.core.asm.inject;

import com.google.common.base.Preconditions;
import lol.farsight.newin.core.annotation.method.Inject;
import lol.farsight.newin.core.asm.gen.EphemeralClassGenerator;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
            final @NotNull MethodVisitor mv,
            final @NotNull String name,
            final int access,
            final @NotNull String targetDesc
    ) {
        int index = 0;
        if ((access & Opcodes.ACC_STATIC) == 0) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);

            index++;
        }

        for (final Type argument : Type.getMethodType(targetDesc).getArgumentTypes()) {
            mv.visitVarInsn(
                    argument.getOpcode(Opcodes.ILOAD),
                    index
            );

            index += argument.getSize();
        }

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                owner,
                name,
                desc,
                false
        );
    }

    public abstract @NotNull MethodVisitor apply(
            final @NotNull EphemeralClassGenerator generator,
            final @NotNull MethodVisitor mv,
            final int access,
            final @NotNull String name,
            final @NotNull String desc
    );
}
