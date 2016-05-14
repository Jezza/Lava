package me.jezza.lava;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Jezza
 */
final class ExtendedArrayList<E> extends ArrayList<E> {

	ExtendedArrayList() {
	}

	ExtendedArrayList(int initialCapacity) {
		super(initialCapacity);
	}

	ExtendedArrayList(Collection<? extends E> c) {
		super(c);
	}

	public E peekLast() {
		int size = size();
		return size == 0 ? null : get(size - 1);
	}

	public E pollLast() {
		int size = size();
		return size == 0 ? null : remove(size - 1);
	}

	public void setSize(int size) {
		if (size < 0)
			throw new IllegalArgumentException("Negative size: " + size);
		if (size == 0) {
			clear();
			return;
		}
		int _size = size();
		if (size >= _size)
			return;
		int diff = _size - size;
		switch (diff) {
			default:
				removeRange(size, _size);
				return;
			case 3:
				pollLast();
			case 2:
				pollLast();
			case 1:
				pollLast();
		}
	}
}
