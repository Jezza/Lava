// $Header: //info.ravenbrook.com/project/jili/version/1.1/test/mnj/lua/HookTest.java#1 $
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

import me.jezza.lava.annotations.SkipSetup;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Jezza
 */
@Ignore("Way too long")
public final class HookTest extends AbstractTest {

	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
		MathLib.open(L);
		OSLib.open(L);
		StringLib.open(L);
		TableLib.open(L);
	}

	@Test
	public void testHookTrial() throws Exception {
		Lua L = this.L;
		loadFile("speed/fannkuch.lua");
		Object script = L.value(-1);
		CallHook hook = new CallHook();
		L.setHook(hook, Lua.MASK_COUNT, 100);
		int status = L.pcall(0, 0, new AddWhere());
		if (status != 0)
			System.out.println(L.value(-1));
		System.out.println("Hook called: " + hook.n + " times");

		hook = new CallHook(true);
		L.setHook(hook, Lua.MASK_COUNT, 1000);
		int x = 0;
		while (true) {
			L.setTop(0);
			L.push(script);
			status = L.resume(0);
			++x;
			if (status != Lua.YIELD)
				break;
		}
		if (status != 0)
			System.out.println(L.value(-1));
		System.out.println("n: " + hook.n + " x: " + x);
	}

	@Test
	@SkipSetup
	public void testHookPreempt() throws Exception {
		String[] script = Speed.scripts;
		int n = script.length;
		Lua[] l = new Lua[n];
		for (int i = 0; i < n; ++i) {
			l[i] = createLua();
			l[i].loadFile("/speed/" + script[i] + ".lua");
			l[i].setHook(new CallHook(true), Lua.MASK_COUNT, 999);
		}

		boolean allFinished;
		while (true) {
			allFinished = true;
			for (int i = 0; i < n; ++i) {
				Lua L = l[i];
				if (L == null)
					continue;
				allFinished = false;
				System.out.print(i);
				int status = L.resume(0);
				if (status != Lua.YIELD) {
					l[i] = null;
					System.out.println("Script " + script[i] + " finished.  Status: " + status);
				}
			}
			if (allFinished)
				break;
		}
		System.out.println("All finished");
	}

	private static class CallHook implements Hook {
		private final boolean yield;
		private int n;

		public CallHook() {
			this(false);
		}

		public CallHook(boolean yield) {
			this.yield = yield;
			this.n = 0;
		}

		@Override
		public int luaHook(Lua L, Debug ar) {
			++n;
			if (yield)
				return L.yield(0);
			return 0;
		}
	}
}
