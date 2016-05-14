/*  $Header: //info.ravenbrook.com/project/jili/version/1.1/code/mnj/lua/LuaTable.java#1 $
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class that models Lua's tables.  Each Lua table is an instance of
 * this class.  Whilst you can clearly see that this class extends
 * {@link java.util.Hashtable} you should in no way rely upon that.
 * Calling any methods that are not defined in this class (but are
 * defined in a super class) is extremely deprecated.
 */
public final class LuaTable {
	private static final Object[] EMPTY = new Object[0];

	private static final int MAX_BITS = 26;
	private static final int MAX_ARRAY_SIZE = 1 << MAX_BITS;

	private static final int MIN_HASH_SIZE = 3;

	/**
	 *
	 */
	private final Map<Object, Object> map;

	/**
	 * Array used so that tables accessed like arrays are more efficient.
	 * All elements stored at an integer index, <var>i</var>, in the
	 * range [1,arrayLength] are stored at <code>array[i-1]</code>.
	 * This speed and space usage for array-like access.
	 * When the table is rehashed the array's size is chosen to be the
	 * largest power of 2 such that at least half the entries are
	 * occupied.  Default access granted for {@link TableIterator} class, do not
	 * abuse.
	 */
	private Object[] array;

	/**
	 * The predicted array size after expansion.
	 * This is used to check if a number should be placed into the array, and what size the array should expand to.
	 */
	private int expansionRate;

	/**
	 * Metatable to fallback on when something wasn't found here.
	 */
	private LuaTable metatable;

	public LuaTable() {
		map = new HashMap<>(MIN_HASH_SIZE);
		array = EMPTY;
		expansionRate = calcExpansionRate(0);
	}

	/**
	 * Fresh LuaTable with hints for preallocating to size.
	 *
	 * @param arrayCount number of array slots to preallocate.
	 * @param hashCount  number of hash slots to preallocate.
	 */
	public LuaTable(int arrayCount, int hashCount) {
		map = new HashMap<>(Math.min(hashCount, MIN_HASH_SIZE));

		if (arrayCount == 0) {
			this.array = EMPTY;
		} else {
			Object[] array = new Object[arrayCount];
			Arrays.fill(array, Lua.NIL);
			this.array = array;
		}
		expansionRate = calcExpansionRate(arrayCount);
	}

	/**
	 * Implements discriminating equality.  <code>o1.equals(o2) == (o1 ==
	 * o2) </code>.  This method is not necessary in CLDC, it's only
	 * necessary in J2SE because java.util.Hashtable overrides equals.
	 *
	 * @param o the reference to compare with.
	 * @return true when equal.
	 */
	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	/**
	 * Provided to avoid Checkstyle warning.  This method is not necessary
	 * for correctness (in neither JME nor JSE), it's only provided to
	 * remove a Checkstyle warning.
	 * Since {@link #equals} implements the most discriminating
	 * equality possible, this method can have any implementation.
	 *
	 * @return an int.
	 */
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return Arrays.toString(array) + ':' + map.toString();
	}

	private static int calcExpansionRate(int oldSize) {
		return oldSize + 1 + (oldSize + 1 >> 1);
	}

	/**
	 * @param newLength - The array will be expanded to this length.
	 */
	private void resize(int newLength) {
		int length = array.length;
		if (newLength == length)
			return;
		Object[] newArray = new Object[newLength];
		if (newLength > length) {
			System.arraycopy(array, 0, newArray, 0, length);
			//	Fills the array with NIL after the initial array copy
			for (int i = 0; i < newLength; i++)
				if (newArray[i] == null)
					newArray[i] = Lua.NIL;
			if (map.size() < newLength - length) {
				Iterator<Entry<Object, Object>> it = map.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Object, Object> entry = it.next();
					Object key = entry.getKey();
					if (key instanceof Double) {
						double d = (Double) key;
						int k = (int) d;
						if (k == d && k >= 1 && k <= newLength) {
							newArray[k - 1] = entry.getValue();
							it.remove();
						}
					}
				}
			} else {
				for (int i = length; i < newLength; ++i) {
					Object v = map.remove((double) (i + 1));
					if (v != null)
						newArray[i] = v;
				}
			}
		} else if (newLength < length) {
			// move elements from array slots nasize to arrayLength-1 to the
			// hash part.
			for (int i = newLength; i < length; ++i)
				if (array[i] != Lua.NIL)
					map.put((double) (i + 1), array[i]);
			System.arraycopy(array, 0, newArray, 0, newArray.length);
		}
		array = newArray;
		expansionRate = calcExpansionRate(newLength);
	}

	/**
	 * Getter for metatable member.
	 *
	 * @return - The metatable that this table uses.
	 */
	LuaTable metatable() {
		return metatable;
	}

	/**
	 * Setter for metatable member.
	 * <p>
	 * :todo: Support metatable's __gc and __mode keys appropriately.
	 * <p>
	 * This involves detecting when those keys are present in the metatable, and changing all the entries in the {@link HashMap} to be instance of java.lang.Ref as appropriate.
	 *
	 * @param metatable - The metatable that this table will use.
	 */
	LuaTable metatable(LuaTable metatable) {
		this.metatable = metatable;
		return this;
	}

	/**
	 * Supports Lua's length (#) operator.  More or less equivalent to luaH_getn and unbound_search in ltable.c.
	 */
	int firstNilIndex() {
		Object[] array = this.array;
		int i = 0;  // Lower bound
		int j = array.length; // Upper bound
		if (j > 0 && array[j - 1] == Lua.NIL) {
			// there is a boundary in the array part: (binary) search for it
			while (j - i > 1) {
				int m = (i + j) / 2; // Midpoint between upper bound and lower bound
				if (array[m - 1] == Lua.NIL) {
					j = m; // Set the upper bound
				} else {
					i = m; // Set the lower bound
				}
			}
			return i;
		}

		// unbound_search
		j = 1;
		// Find 'i' and 'j' such that i is present and j is not.
		while (get(j) != Lua.NIL) {
			i = j;
			j *= 2;
			if (j < 0) {        // overflow
				// Pathological case.  Linear search.
				i = 1;
				while (get(i) != Lua.NIL) {
					++i;
				}
				return i - 1;
			}
		}
		// binary search between i and j
		while (j - i > 1) {
			int m = (i + j) / 2;
			if (get(m) == Lua.NIL) {
				j = m;
			} else {
				i = m;
			}
		}
		return i;
	}

	/**
	 * Like get for numeric (integer) keys.
	 */
	Object get(int k) {
		if (k >= 1 && k <= array.length)
			return array[k - 1];
		return map.getOrDefault((double) k, Lua.NIL);
	}

	/**
	 * Like {@link java.util.Hashtable#get}.  Ensures that indexes
	 * with no value return {@link Lua#NIL}.  In order to get the correct
	 * behaviour for <code>t[nil]</code>, this code assumes that Lua.NIL
	 * is non-<code>null</code>.
	 */
	Object get(Object key) {
		if (key instanceof Double) {
			double d = (Double) key;
			int k = (int) d;
			if (k == d && k >= 1 && k <= array.length)
				return array[k - 1];
		}
		return map.getOrDefault(key, Lua.NIL);  // 'key' did not match some condition
	}

	/**
	 * Like {@link #get(Object)} but the result is written into
	 * the <var>value</var> {@link Slot}.
	 */
	void get(Slot key, Slot value) {
		if (key.t == Lua.TNUMBER) {
			double d = key.d;
			int i = (int) d;
			if (i == d && d >= 1 && d <= array.length) {
				value.setObject(array[i - 1]);
				return;
			}
		}
		value.setObject(map.getOrDefault(key.asObject(), Lua.NIL));
	}

	/**
	 * Handles the Lua.NIL case of a map value. (Removes it from the map.)
	 *
	 * @param key   key with which the specified value is to be associated
	 * @param value value to be associated with the specified key, or Lua.NIL, then the mapping will be removed.
	 */
	private void _put(Object key, Object value) {
		if (value != Lua.NIL) {
			map.put(key, value);
		} else {
			map.remove(key);
		}
	}

	/**
	 * Like put for numeric (integer) keys.
	 */
	void put(int k, Object v) {
		if (k >= 1 && k <= expansionRate) {
			if (k > array.length)
				resize(expansionRate);
			array[k - 1] = v;
		} else {
			_put((double) k, v);
		}
	}

	/**
	 * Like {@link java.util.Hashtable#put} but enables Lua's semantics for <code>nil</code>;
	 * In particular that <code>x = nil</nil>
	 * deletes <code>x</code>.
	 * And also that <code>t[nil]</code> raises an error.
	 * Generally, users of Jill should be using
	 * {@link Lua#setTable} instead of this.
	 *
	 * @param key   key.
	 * @param value value.
	 */
	void put(Lua L, Object key, Object value) {
		if (key == Lua.NIL)
			throw L.gRunerror("table index is nil");
		if (key instanceof Double) {
			double d = (Double) key;
			if (Double.isNaN(d))
				throw L.gRunerror("table index is NaN");
			int k = (int) d;
			if (k == d && k >= 1 && k <= expansionRate) {
				if (k > array.length)
					resize(expansionRate);
				array[k - 1] = value;
				return;
			}
		}
		_put(key, value);
	}

	void put(Lua L, Slot key, Object value) {
		if (key.t == Lua.TNUMBER) {
			double d = key.d;
			if (Double.isNaN(d))
				throw L.gRunerror("table index is NaN");
			int k = (int) d;
			if (k == d && k >= 1 && k <= expansionRate) {
				if (k > array.length)
					resize(expansionRate);
				array[k - 1] = value;
				return;
			}
		}
		Object k = key.asObject();
		if (k == Lua.NIL)
			throw L.gRunerror("table index is nil");
		_put(k, value);
	}

	public Iterator<Object> keys() {
		return new TableIterator();
	}

	private class TableIterator implements Iterator<Object> {
		private Iterator<Object> it = map.keySet().iterator();
		private int i;

		private TableIterator() {
			increment();
		}

		@Override
		public boolean hasNext() {
			return i < array.length || it.hasNext();
		}

		@Override
		public Object next() {
			if (i < array.length) {
				// array index i corresponds to key i + 1
				double value = ++i;
				increment();
				return value;
			}
			return it.next();
		}

		/**
		 * Increments {@link #i} until it either exceeds
		 * <code>arrayLength</code> or indexes a non-nil element.
		 */
		private void increment() {
			Object[] array = LuaTable.this.array;
			int l = array.length;
			while (i < l && array[i] == Lua.NIL)
				++i;
		}
	}
}
