package raven.modal.demo.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Must be available at runtime
@Target(ElementType.FIELD)       // Can only be applied to fields
public @interface ColumnName {

    /**
     * The exact name of the database column (case-insensitive mapping will be applied).
     */
    String value();
}