package lol.farsight.newin.api.annotation.method;

import lol.farsight.newin.api.annotation.other.At;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Inject {
    String name();

    At at();
}
