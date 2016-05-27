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
 * Contains Lua's IO library.
 * The library can be opened using the {@link #open} method.
 *
 * @author Jezza
 */
public class IOLib {

	public static void open(Lua L) {
		LuaTable lib = L.register("io");

		r(L, lib, "close", IOLib::nyi);
		r(L, lib, "flush", IOLib::nyi);
		r(L, lib, "input", IOLib::nyi);
		r(L, lib, "lines", IOLib::nyi);
		r(L, lib, "open", IOLib::_open);
		r(L, lib, "output", IOLib::nyi);
		r(L, lib, "popen", IOLib::nyi);
		r(L, lib, "read", IOLib::nyi);
		r(L, lib, "stderr", IOLib::nyi);
		r(L, lib, "stdin", IOLib::nyi);
		r(L, lib, "stdout", IOLib::nyi);
		r(L, lib, "tmpfile", IOLib::nyi);
		r(L, lib, "type", IOLib::nyi);
		r(L, lib, "write", IOLib::nyi);
	}

	/**
	 * Register a function.
	 */
	private static void r(Lua L, LuaTable lib, String name, LuaJavaCallback function) {
		L.setField(lib, name, function);
	}

	private static int _open(Lua L) {
		String fileName = L.checkString(1);
		String mode = L.optString(2, "r");

//		LuaUserdata userdata = new LuaUserdata();

		return 0;
	}

	private static int nyi(Lua L) {
		throw L.gRunError("not yet implemented");
	}
}
