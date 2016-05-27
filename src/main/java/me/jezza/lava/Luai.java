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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

public final class Luai {
	public static void main(String[] arg) throws Exception {
		if (arg.length == 0)
			throw new IllegalArgumentException("Need to provide a file name");
		String name = arg[0];

		Lua L;
		int status;
		try (InputStream in = new BufferedInputStream(new FileInputStream(name))) {
			L = new Lua();
			BaseLib.open(L);
			PackageLib.open(L);
			MathLib.open(L);
			OSLib.open(L);
			StringLib.open(L);
			TableLib.open(L);

			status = L.load(in, "@" + name);
			if (status != 0)
				throw new Exception("Failed to load '" + name + "': " + L.value(1));
		}

		status = L.pcall(0, Lua.MULTRET, Lua.ADD_STACK_TRACE);
		if (status != 0)
			System.out.println(L.value(-1));
	}
}
