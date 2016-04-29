package me.jezza.luja;

import java.util.LinkedList;

/**
 * @author Jezza
 */
final class ChainedList<E> extends LinkedList<E> {

	void clip(int length) {
		if (length > 0) {
			while (size() > length)
				pollLast();
		}
	}
}
