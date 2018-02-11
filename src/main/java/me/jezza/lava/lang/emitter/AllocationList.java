package me.jezza.lava.lang.emitter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jezza
 */
public class AllocationList {
	private final List<Object> list;

	public AllocationList() {
		this.list = new ArrayList<>();
	}

	public int registerAny() {
		int index = list.size();
		list.add("<any>");
		return index;
	}

	public int register(Object o) {
		int i = indexOf(o);
		if (i == -1) {
			i = list.size();
			list.add(o);
		}
		return i;
	}

	public void free(int value) {
		Object removed = list.remove(value);
		System.out.println("Removed: " + removed);
	}

	public int size() {
		return list.size();
	}

	public int indexOf(Object value) {
		return list.indexOf(value);
	}
}