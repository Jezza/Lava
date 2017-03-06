package me.jezza.lava;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * @author Jezza
 */
final class Bypass {

	static final Lookup LOOKUP;

	static final Unsafe UNSAFE;

	static {
		try {
			Field field = Lookup.class.getDeclaredField("IMPL_LOOKUP");
			field.setAccessible(true);
			LOOKUP = (Lookup) field.get(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}

		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			UNSAFE = (Unsafe) field.get(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private Bypass() {
		throw new IllegalStateException();
	}
}
