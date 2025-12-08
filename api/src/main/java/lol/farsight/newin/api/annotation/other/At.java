package lol.farsight.newin.api.annotation.other;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ })
public @interface At {
    enum Point {
        HEAD,
        EXIT
    }

    Point point();
}
