package lol.farsight.newin.core;

import com.google.common.base.Preconditions;
import lol.farsight.newin.core.asm.NewinClassVisitor;
import lol.farsight.newin.core.transformer.BcTransformer;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;

public final class NewinRegistrar {
    public static final NewinRegistrar INSTANCE = new NewinRegistrar();

    private NewinRegistrar() {}

    public void applyFromClasses(final @NotNull Class<?> @NotNull ... classes) {
        Preconditions.checkNotNull(classes, "classes");

        for (int i = 0, classesLength = classes.length; i < classesLength; i++) {
            final var cl = classes[i];
            Preconditions.checkNotNull(cl, "element " + i + " of classes");

            final var newinCv = new NewinClassVisitor(cl);
            new ClassReader(
                    BcTransformer.bytes(cl)
            ).accept(newinCv, 0);

            newinCv.apply();
        }
    }
}
