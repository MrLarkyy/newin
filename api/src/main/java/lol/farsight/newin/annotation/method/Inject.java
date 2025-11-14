package lol.farsight.newin.annotation.method;

import lol.farsight.newin.annotation.other.At;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Inject {
    String name();

    At at();
}
