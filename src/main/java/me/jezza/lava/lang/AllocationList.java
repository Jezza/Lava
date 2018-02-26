package me.jezza.lava.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jezza
 */
public final class AllocationList {
	private static final String ANY = "<any>";
	private static final String FREE = "<free>";
	
	private final List<Object> list;

	public AllocationList() {
		this.list = new ArrayList<>();
	}

	public int allocate() {
		int index = list.indexOf(FREE);
		if (index == -1) {
			index = list.size();
			list.add(ANY);
		} else {
			list.set(index, ANY);
		}
		return index;
	}
	
	public int allocate(int count) {
		int index = list.size();
		for (int i = 0; i < count; i++) {
			list.add(ANY);
		}
		return index;
	}

	public int allocate(Object o) {
		int i = indexOf(o);
		return i != -1
				? i
				: allocate();
	}
	
//	public int pop() {
//	}

	public void free(int value) {
		list.set(value, FREE);
	}
	
	public int max() {
		return list.size();
	}

	public int size() {
		return list.size();
	}

	public int indexOf(Object value) {
		return list.indexOf(value);
	}
}