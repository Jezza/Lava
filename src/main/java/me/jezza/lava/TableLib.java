/*  $Header: //info.ravenbrook.com/project/jili/version/1.1/code/mnj/lua/TableLib.java#1 $
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

import java.util.Iterator;

/**
 * Contains Lua's table library.
 * The library can be opened using the {@link #open} method.
 */
public final class TableLib {
	/**
	 * Opens the string library into the given Lua state.  This registers
	 * the symbols of the string library in a newly created table called
	 * "string".
	 *
	 * @param L The Lua state into which to open.
	 */
	public static void open(Lua L) {
		LuaTable t = L.register("table");

		r(L, t, "concat", TableLib::concat);
		r(L, t, "insert", TableLib::insert);
		r(L, t, "maxn", TableLib::maxn);
		r(L, t, "remove", TableLib::remove);
		r(L, t, "sort", TableLib::sort);
	}

	/**
	 * Register a function.
	 */
	private static void r(Lua L, LuaTable t, String name, LuaJavaCallback function) {
		L.setField(t, name, function);
	}

	/**
	 * Private constructor.
	 */
	private TableLib() {
		throw new IllegalStateException();
	}

	/**
	 * Implements table.concat.
	 */
	private static int concat(Lua L) {
		String sep = L.optString(2, "");
		L.checkType(1, Lua.TTABLE);
		int i = L.optInt(3, 1);
		int last = L.optInt(4, Lua.objLen(L.value(1)));
		StringBuilder b = new StringBuilder();
		Object t = L.value(1);
		for (; i <= last; ++i) {
			Object v = Lua.rawGetI(t, i);
			L.argCheck(Lua.isString(v), 1, "table contains non-strings");
			b.append(L.toString(v));
			if (i != last && !sep.isEmpty())
				b.append(L.toString(sep));
		}
		L.pushString(b.toString());
		return 1;
	}

	/**
	 * Implements table.insert.
	 */
	private static int insert(Lua L) {
		int e = aux_getn(L, 1) + 1; // first empty element
		int pos;    // where to insert new element
		Object t = L.value(1);
		switch (L.getTop()) {
			case 2:   // called with only 2 arguments
				pos = e;        // insert new element at the end
				break;

			case 3:
				pos = L.checkInt(2);  // 2nd argument is the position
				// grow array if necessary, and move up elements
				if (pos > e)
					e = pos;
				for (int i = e; i > pos; --i)    // t[i] = t[i-1]
					L.rawSetI(t, i, Lua.rawGetI(t, i - 1));
				break;

			default:
				throw L.error("wrong number of arguments to 'insert'");
		}
		L.rawSetI(t, pos, L.value(-1));     // t[pos] = v
		return 0;
	}

	/**
	 * Implements table.maxn.
	 */
	private static int maxn(Lua L) {
		double max = 0;
		L.checkType(1, Lua.TTABLE);
		LuaTable t = (LuaTable) L.value(1);
		Iterator<Object> keys = t.keys();
		while (keys.hasNext()) {
			Object o = keys.next();
			if (Lua.type(o) == Lua.TNUMBER) {
				double v = Lua.toNumber(o).orElse(0D);
				if (v > max)
					max = v;
			}
		}
		L.pushNumber(max);
		return 1;
	}

	/**
	 * Implements table.remove.
	 */
	private static int remove(Lua L) {
		int e = aux_getn(L, 1);
		if (e == 0)
			return 0;         // table is 'empty'
		int pos = L.optInt(2, e);
		Object t = L.value(1);
		Object o = Lua.rawGetI(t, pos);       // result = t[pos]
		for (; pos < e; ++pos)
			L.rawSetI(t, pos, Lua.rawGetI(t, pos + 1));   // t[pos] = t[pos+1]
		L.rawSetI(t, e, Lua.NIL);   // t[e] = nil
		L.push(o);
		return 1;
	}

	/**
	 * Implements table.sort.
	 */
	private static int sort(Lua L) {
		int n = aux_getn(L, 1);
		if (!L.isNoneOrNil(2))      // is there a 2nd argument?
			L.checkType(2, Lua.TFUNCTION);
		L.setTop(2);        // make sure there is two arguments
		auxsort(L, 1, n);
		return 0;
	}

	static void auxsort(Lua L, int l, int u) {
		Object t = L.value(1);
		while (l < u)       // for tail recursion
		{
			int i;
			int j;
			// sort elements a[l], a[l+u/2], and a[u]
			Object o1 = Lua.rawGetI(t, l);
			Object o2 = Lua.rawGetI(t, u);
			if (sort_comp(L, o2, o1)) // a[u] < a[l]?
			{
				L.rawSetI(t, l, o2);
				L.rawSetI(t, u, o1);
			}
			if (u - l == 1)
				break;  // only 2 elements
			i = (l + u) / 2;
			o1 = Lua.rawGetI(t, i);
			o2 = Lua.rawGetI(t, l);
			if (sort_comp(L, o1, o2)) // a[i]<a[l]?
			{
				L.rawSetI(t, i, o2);
				L.rawSetI(t, l, o1);
			} else {
				o2 = Lua.rawGetI(t, u);
				if (sort_comp(L, o2, o1))       // a[u]<a[i]?
				{
					L.rawSetI(t, i, o2);
					L.rawSetI(t, u, o1);
				}
			}
			if (u - l == 2)
				break;  // only 3 elements
			final Object p = Lua.rawGetI(t, i); // Pivot
			o2 = Lua.rawGetI(t, u - 1);
			L.rawSetI(t, i, o2);
			L.rawSetI(t, u - 1, p);
			// a[l] <= P == a[u-1] <= a[u], only need to sort from l+1 to u-2
			i = l;
			j = u - 1;
			// NB: Pivot P is in p
			while (true)      // invariant: a[l..i] <= P <= a[j..u]
			{
				// repeat ++i until a[i] >= P
				while (true) {
					o1 = Lua.rawGetI(t, ++i);
					if (!sort_comp(L, o1, p))
						break;
					if (i > u)
						throw L.error("invalid order function for sorting");
				}
				// repreat --j until a[j] <= P
				while (true) {
					o2 = Lua.rawGetI(t, --j);
					if (!sort_comp(L, p, o2))
						break;
					if (j < l)
						throw L.error("invalid order function for sorting");
				}
				if (j < i)
					break;
				L.rawSetI(t, i, o2);
				L.rawSetI(t, j, o1);
			}
			o1 = Lua.rawGetI(t, u - 1);
			o2 = Lua.rawGetI(t, i);
			L.rawSetI(t, u - 1, o2);
			L.rawSetI(t, i, o1);      // swap pivot (a[u-1]) with a[i]
			// a[l..i-1 <= a[i] == P <= a[i+1..u]
			// adjust so that smaller half is in [j..i] and larger one in [l..u]
			if (i - l < u - i) {
				j = l;
				i = i - 1;
				l = i + 2;
			} else {
				j = i + 1;
				i = u;
				u = j - 2;
			}
			auxsort(L, j, i); // call recursively the smaller one
		} // repeat the routine for the larger one
	}

	private static boolean sort_comp(Lua L, Object a, Object b) {
		// function?
		if (!Lua.isNil(L.value(2))) {
			L.pushValue(2);
			L.push(a);
			L.push(b);
			L.call(2, 1);
			boolean res = Lua.toBoolean(L.value(-1));
			L.pop(1);
			return res;
		}
		return L.lessThan(a, b);
	}

	private static int aux_getn(Lua L, int n) {
		L.checkType(n, Lua.TTABLE);
		return ((LuaTable) L.value(n)).firstNilIndex();
	}
}
