package listeners;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker annotation to indicate that a test method should be skipped
 * in production or restricted environments.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface NoProd {
    // No fields required - serves as a marker
}

