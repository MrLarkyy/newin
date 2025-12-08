package lol.farsight.newin.core.asm.inject;

import com.google.common.base.Preconditions;
import lol.farsight.newin.core.asm.gen.EphemeralClassGenerator;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public final class HeadInjector extends Injector {
    public HeadInjector(
            final @NotNull String owner,
            final int access,
            final @NotNull String name,
            final @NotNull String desc
    ) {
        super(owner, access, name, desc);
    }

    @Override
    public @NotNull MethodVisitor apply(
            final @NotNull EphemeralClassGenerator generator,
            final @NotNull MethodVisitor mv,
            final int access,
            final @NotNull String name,
            final @NotNull String desc
    ) {
        Preconditions.checkNotNull(generator, "generator");
        Preconditions.checkNotNull(mv, "mv");
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(desc, "desc");

        final String injectionName = generator.injection(this.access, this.name, this.desc);
        return new AdviceAdapter(Opcodes.ASM9, mv, access, name, desc) {
            @Override
            protected void onMethodEnter() {
                write(mv, injectionName, access, desc);
            }
        };
    }
}
