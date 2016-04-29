// $Header: //info.ravenbrook.com/project/jili/version/1.1/test/mnj/lua/JiliTestCase.java#1 $
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

package me.jezza.luja;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Common superclass for all Jill's (j2meunit) tests.
 */
class JiliTestCase implements Test {
	private final String name;

	JiliTestCase() {
		this.name = getClass().getSimpleName() + ":Unknown";
	}

	JiliTestCase(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void run() {
		suite().cases().forEach(Test::runTest);
	}

	public void runTest() {
	}

	/**
	 * Loads file and leaves LuaFunction on the stack.  Fails the test if
	 * there was a problem loading the file.
	 *
	 * @param L        Lua state in which to load file.
	 * @param filename filename without '.luc' extension.
	 */
	protected void loadFile(Lua L, String filename) {
		String suffix[] = {"", ".lua", ".luc"};
		InputStream is = null;
		String s = "";
		for (String aSuffix : suffix) {
			s = filename + aSuffix;
			is = getClass().getResourceAsStream("/" + s);
			if (is != null)
				break;
		}
		System.out.println("Loading: " + filename);
		int status = L.load(is, "@" + s);
		assertTrue("Loaded " + filename + " ok", status == 0);
	}

	protected void assertTrue(boolean condition) {
		assertTrue("Condition for " + name + " not met!", condition);
	}

	protected void assertTrue(String message, boolean condition) {
		if (!condition)
			throw new IllegalStateException(message);
	}

	protected void assertNotNull(Object object) {
		if (object == null)
			throw new IllegalStateException("Object is null");
	}

	protected void assertNotNull(String message, Object object) {
		if (object == null)
			throw new IllegalStateException(message);
	}

	protected void assertSame(String message, Object first, Object second) {
		if (!first.equals(second))
			throw new IllegalStateException(message);
	}

	protected void loadFileAndRun(Lua L, String file, String name, int n) {
		loadFile(L, file);
		L.call(0, 0);
		System.out.println(name);
		L.push(L.getGlobal(name));
		int status = L.pcall(0, n, new AddWhere());
		if (status != 0) {
			System.out.println(L.toString(L.value(-1)));
		}
		assertTrue(name, status == 0);
	}

	public Test suite() {
		return null;
	}

	/**
	 * Compiles/loads file and leaves LuaFunction on the stack.  Fails the test
	 * if there was a problem loading the file.
	 *
	 * @param L        Lua state in which to load file.
	 * @param filename filename without '.luc' extension.
	 */
	protected void compileLoadFile(Lua L, String filename) {
		filename += ".lua";
		System.out.println(filename);
		InputStream is = getClass().getResourceAsStream("/" + filename);
		assertTrue("Found " + filename + " ok", is != null);
		int status = L.load(is, filename);
		assertTrue("Compiled/loaded " + filename + " ok", status == 0);
	}

	@Override
	public List<Test> cases() {
		return Collections.emptyList();
	}
}

final class AddWhere extends LuaJavaCallback {

	@Override
	public int luaFunction(Lua L) {
		boolean any = false;
		for (int i = 1; i <= 3; ++i) {
			String s = L.where(i);
			if (!s.equals("")) {
				if (any)
					s = s + " > ";
				any = true;
				L.insert(s, -1);
				L.concat(2);
			}
		}
		return 1;
	}
}
