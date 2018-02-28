package me.jezza.lava.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Jezza
 */
public class LTable {
	private static final Object[] EMPTY = new Object[0];

	private static final int MIN_HASH_SIZE = 3;

	/**
	 * The internal {@link HashMap} that the table uses when the key isn't an index.
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
	 * When not null, used as the fallback when something wasn't found in this table.
	 */
	private LTable fallback;

	public LTable() {
		map = new HashMap<>(MIN_HASH_SIZE);
		array = EMPTY;
		expansionRate = calcExpansionRate(0);
	}

	/**
	 * Fresh LuaTable with hints for preallocating to size.
	 *
	 * @param initialArray
	 * 		number of array slots to preallocate.
	 * @param initialHash
	 * 		number of hash slots to preallocate.
	 */
	public LTable(int initialArray, int initialHash) {
		map = new HashMap<>(Math.min(initialHash, MIN_HASH_SIZE));

		// @CLEANUP Jezza - 28 Feb 2018: Pos check?
		this.array = initialArray > 0
				? new Object[initialArray]
				: EMPTY;
		expansionRate = calcExpansionRate(initialArray);
	}

	public LTable fallback() {
		return fallback;
	}

	public LTable fallback(LTable value) {
		this.fallback = value != this
				? value
				: null;
		return this;
	}
	
	/**
	 * @param key   key with which the specified value is to be associated
	 * @param value value to be associated with the specified key, or null, then the mapping will be removed.
	 */
	public void put(Object key, Object value) {
		if (key == null) {
			// @CLEANUP Jezza - 28 Feb 2018: yuck
			throw new IllegalStateException("Null key value");
		}
		if (key instanceof Integer) {
			Integer intKey = ((Integer) key);
		} else if (key instanceof Double) {
			Double doubleKey = (Double) key;
			if (Double.isNaN(doubleKey)) {
				// @CLEANUP Jezza - 28 Feb 2018: yuck
				throw new IllegalStateException("key is NaN");
			}
		} else if (value != null) {
			map.put(key, value);
		} else {
			map.remove(key);
		}
	}
	
	private void putInt(int key, Object value) {
		
	}
	
	

//	/**
//	 * Like put for numeric (integer) keys.
//	 */
//	void put(int k, Object v) {
//		if (k >= 1 && k <= expansionRate) {
//			if (k > array.length)
//				resize(expansionRate);
//			array[k - 1] = v;
//		} else {
//			_put((double) k, v);
//		}
//	}
//
//	/**
//	 * Like {@link java.util.Hashtable#put} but enables Lua's semantics for <code>nil</code>;
//	 * In particular that <code>x = nil</nil>
//	 * deletes <code>x</code>.
//	 * And also that <code>t[nil]</code> raises an error.
//	 * Generally, users of Jill should be using
//	 * {@link Lua#setTable} instead of this.
//	 *
//	 * @param key   key.
//	 * @param value value.
//	 */
//	void put(Lua L, Object key, Object value) {
//		if (key == Lua.NIL)
//			throw L.gRunError("table index is nil");
//		if (key instanceof Double) {
//			double d = (Double) key;
//			if (Double.isNaN(d))
//				throw L.gRunError("table index is NaN");
//			int k = (int) d;
//			if (k == d && k >= 1 && k <= expansionRate) {
//				if (k > array.length)
//					resize(expansionRate);
//				array[k - 1] = value;
//				return;
//			}
//		}
//		_put(key, value);
//	}
//
//	void put(Lua L, Slot key, Object value) {
//		if (key.t == Lua.TNUMBER) {
//			double d = key.d;
//			if (Double.isNaN(d))
//				throw L.gRunError("table index is NaN");
//			int k = (int) d;
//			if (k == d && k >= 1 && k <= expansionRate) {
//				if (k > array.length)
//					resize(expansionRate);
//				array[k - 1] = value;
//				return;
//			}
//		}
//		Object k = key.asObject();
//		if (k == Lua.NIL)
//			throw L.gRunError("table index is nil");
//		_put(k, value);
//	}

//	/**
//	 * @param newLength - The array will be expanded to this length.
//	 */
//	private void resize(int newLength) {
//		int length = array.length;
//		if (newLength == length)
//			return;
//		Object[] newArray = new Object[newLength];
//		if (newLength > length) {
//			System.arraycopy(array, 0, newArray, 0, length);
//			//	Fills the array with NIL after the initial array copy
//			for (int i = 0; i < newLength; i++)
//				if (newArray[i] == null)
//					newArray[i] = Lua.NIL;
//			if (map.size() < newLength - length) {
//				Iterator<Entry<Object, Object>> it = map.entrySet().iterator();
//				while (it.hasNext()) {
//					Entry<Object, Object> entry = it.next();
//					Object key = entry.getKey();
//					if (key instanceof Double) {
//						double d = (Double) key;
//						int k = (int) d;
//						if (k == d && k >= 1 && k <= newLength) {
//							newArray[k - 1] = entry.getValue();
//							it.remove();
//						}
//					}
//				}
//			} else {
//				for (int i = length; i < newLength; ++i) {
//					Object v = map.remove((double) (i + 1));
//					if (v != null)
//						newArray[i] = v;
//				}
//			}
//		} else if (newLength < length) {
//			// move elements from array slots nasize to arrayLength-1 to the
//			// hash part.
//			for (int i = newLength; i < length; ++i)
//				if (array[i] != Lua.NIL)
//					map.put((double) (i + 1), array[i]);
//			System.arraycopy(array, 0, newArray, 0, newArray.length);
//		}
//		array = newArray;
//		expansionRate = calcExpansionRate(newLength);
//	}

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
			Object[] array = LTable.this.array;
			int l = array.length;
//			while (i < l && array[i] == Lua.NIL)
//				++i;
		}
	}
	
	@Override
	public boolean equals(Object o) {
		return this == o;
	}

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
}
