/**
 * Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject
 * to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package me.jezza.lava;

import java.util.ArrayList;

/**
 * @author Jezza
 */
final class ExtendedArrayList<E> extends ArrayList<E> {
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
