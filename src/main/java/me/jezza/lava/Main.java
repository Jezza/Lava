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

/**
 * TODO Remove this class, it's only temporary so I can run some quick code benchmarks without ruining a class.
 *
 * @author Jezza
 */
public final class Main {

	private static final int UPPER_LIMIT = 50;

	public static void main(String[] args) {
		Lua L = new Lua();
		BaseLib.open(L);

		L.doString("local a = {\"Test\"}\n" +
				"\n" +
				"local b = 0\n" +
				"\n" +
				"a[b], b = \"Hello\", \"World\"\n" +
				"\n" +
				"print(a, \":::\", b);\n");
	}

	public static void main0(String[] args) throws Exception {
		Lua L = new Lua();
		BaseLib.open(L);
		MathLib.open(L);
		L.register("lol", _L -> {
			System.out.println("Hello!");
			L.pushBoolean(true);
			return 1;
		});

		L.doString("local x,y,z; x, y, z = \"\";");



//		LuaTable t = new LuaTable();
//		LuaTable first = new LuaTable();
//		LuaTable second = new LuaTable();
//		LuaTable third = new LuaTable();
//		L.rawSet(t, "first", first);
//		L.rawSet(first, "second", second);
//		L.rawSet(second, "third", third);
//		L.findTable(t, "first.second.third", 2);
//		assert(third == L.value(-1));

//		Lua L = new Lua();
//		int status = L.doString("io.write('asd')");
//		if (status != 0)
//			System.out.println(L.value(-1));

//		LuaTable t = new LuaTable();
//		t.put(33, "initial");
//		System.out.println(t);
//		for (int i = 1; i <= 39; i++) {
//			t.put(i, Integer.toString(i));
//			System.out.println(t);
//		}


//		expansionTest("x << 1                ", x -> x << 1);
//		expansionTest("x + (x >> 1)          ", x -> Math.max(x + (x >> 1), 2));
//		expansionTest("x + (x >> 1) + 1      ", x -> x + (x >> 1) + 1);
//		expansionTest("x + (x >> 1)          ", x -> x + (x >> 1));
//		expansionTest("x + 1 + (x + 1 >> 1)  ", x -> x + 1 + (x + 1 >> 1));
//		expansionTest("x * 2 + 1             ", x -> x * 2 + 1);
	}

	public static void expansionTest(String name, IntTransform transform) {
		StringBuilder axis = new StringBuilder();
		StringBuilder values = new StringBuilder();

		for (int i = 0; i < name.length(); i++)
			axis.append(' ');
		values.append(name);
		for (int i = 0; i < 2; i++) {
			axis.append(' ');
			values.append(' ');
		}
		axis.append("| ");
		values.append("| ");

		for (int i = 0; i < UPPER_LIMIT; i++) {
			int t = transform.apply(i);
			values.append(t);
			axis.append(i);

			if (i < UPPER_LIMIT - 1) {
				int transLength = stringSize(t);
				int axisLength = stringSize(i);
				values.append(' ');
				if (transLength >= axisLength) {
					for (int j = 0; j < (transLength + 1 - axisLength); j++)
						axis.append(' ');
				} else {
					for (int j = 0; j < (axisLength + 1 - transLength); j++)
						axis.append(' ');
				}
			}
		}

		System.out.println(axis);
		System.out.println(values);
		System.out.println();
	}

	static final int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE};

	// Requires positive x
	static int stringSize(int x) {
		for (int i = 0; ; i++)
			if (x <= sizeTable[i])
				return i + 1;
	}

	@FunctionalInterface
	public interface IntTransform {
		int apply(int x);
	}
}
