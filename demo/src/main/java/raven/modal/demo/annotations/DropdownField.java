package raven.modal.demo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Important: must be available at runtime
@Target(ElementType.FIELD)       // Can only be applied to fields
public @interface DropdownField {

    /**
     * Marks the field that holds the primary ID (e.g., CompanyId, which will be set to 0).
     */
    boolean isId() default false;

    /**
     * Marks the field that holds the display text (e.g., CompanyName, which will be set to the default string).
     */
    boolean isText() default false;
}
