package me.jezza.lava.lang.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jezza
 */
public final class ConstantPool<T> {
	private final List<T> pool;

	public ConstantPool() {
		this.pool = new ArrayList<>();
	}

	public int size() {
		return pool.size();
	}

	public int add(T o) {
		int i = indexOf(o);
		if (i == -1) {
			i = pool.size();
			pool.add(o);
		}
		return i;
	}

	public Object[] build() {
		return pool.toArray(new Object[0]);
	}
	
	public T[] build(T[] array) {
		return pool.toArray(array);
	}

	public int indexOf(T value) {
		return pool.indexOf(value);
	}
}