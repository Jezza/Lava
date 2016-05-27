// $Header: //info.ravenbrook.com/project/jili/version/1.1/test/mnj/lua/Speed.java#1 $
// Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
// All rights reserved.
// 
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject
// to the following conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
// ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
// CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package me.jezza.lava;

final class Speed {

	public static void main(String[] arg) {
		System.out.println(report());
	}

	static final String[] scripts = {
			"binarytrees",
			"fannkuch",
			"nbody",
			"nsieve",
			"partialsums",
			"recursive",
			"spectralnorm",
	};

	static String report() {
		StringBuilder b = new StringBuilder();

		long total = 0;
		for (String script : scripts) {
			long t = execute(script);
			b.append(script);
			b.append(": ");
			b.append(t);
			b.append(" ns or ");
			b.append(t / 1000000000D);
			b.append(" seconds.");
			b.append('\n');
			total += t;
		}

		b.append("Total: ");
		b.append(total);
		b.append(" : ");
		b.append(total / 1000000000D);
		b.append(" seconds.");
		b.append('\n');
		return b.toString();
	}

	/**
	 * @return execution time in nanoseconds.
	 */
	static long execute(String name) {
		Lua L = new Lua();
		BaseLib.open(L);
		PackageLib.open(L);
		MathLib.open(L);
		OSLib.open(L);
		StringLib.open(L);
		TableLib.open(L);

		long start = System.nanoTime();
		L.loadFile("/speed/" + name + ".lua");
		int status = L.pcall(0, 0, Lua.ADD_STACK_TRACE);
		if (status != 0)
			System.out.println(L.value(-1));
		return System.nanoTime() - start;
	}
}
