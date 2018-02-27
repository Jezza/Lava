package me.jezza.lava.lang.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jezza
 */
public final class ConstantPool {
	private final List<Object> pool;

	public ConstantPool() {
		this.pool = new ArrayList<>();
	}

	public int size() {
		return pool.size();
	}

	public int add(Object o) {
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

	public int indexOf(Object value) {
		return pool.indexOf(value);
	}
}