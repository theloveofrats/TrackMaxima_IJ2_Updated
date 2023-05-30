package beats.ltr6.imagej;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Preference {

    public PreferenceType value() default PreferenceType.TEXT_FIELD;
}
