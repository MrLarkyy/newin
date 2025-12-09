package lol.farsight.newin.core.asm.annotation;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum NewinError {
    MULTIPLE_ANNOTATIONS("Multiple newin-type annotations found"),
    NO_ANNOTATIONS("No newin-type annotations found"),
    NON_PUBLIC("Non-public method with newin-type annotations found"),
    NON_VOID("Method with return type other the void found"),
    INVALID_CONSTRUCTOR("Constructor signature is invalid, expected public constructor with no parameters"),
    NON_STANDARD("Non-standard method (like the constructor or <clinit>) with newin-type annotations found");

    private static final Logger LOGGER = LoggerFactory.getLogger(NewinError.class);

    private final @NotNull String message;

    NewinError(final @NotNull String message) {
        this.message = message;
    }

    public void log(
            final @NotNull String newinName,
            final @NotNull String methodName
    ) {
        Preconditions.checkNotNull(newinName, "newinName");
        Preconditions.checkNotNull(methodName, "methodName");

        LOGGER.error("Error at method \"{}\" in newin class \"{}\": {}", methodName, newinName, message);
    }
}