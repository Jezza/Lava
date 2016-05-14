package me.jezza.lava.annotations;

import java.lang.annotation.*;

/**
 * @author Jezza
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Libraries.class)
public @interface Library {
	Class<?> value();

	class None {
	}
}
