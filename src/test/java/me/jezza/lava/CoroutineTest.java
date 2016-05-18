package me.jezza.lava;

import me.jezza.lava.annotations.Call;
import me.jezza.lava.annotations.Library;
import me.jezza.lava.annotations.Library.None;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for general coroutine functions.
 * <p>
 * Auxiliary files:
 * CoroutineTest.lua
 *
 * @author Jezza
 */
public class CoroutineTest extends AbstractTest {
	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
		StringLib.open(L);
		TableLib.open(L);
	}

	/**
	 * Ordinary completion of a thread.
	 */
	@Test
	@Library(BaseLib.class)
	public void test1() throws Exception {
		L.push(L.getGlobal("tostring"));
		L.push("hello");
		int status = L.resume(1);
		Assert.assertTrue("Status is not 0", status == 0);
		Assert.assertTrue("Number of arguments is not 1", L.getTop() == 1);
	}

	/**
	 * Thread that yields.
	 */
	@Test
	@Library(BaseLib.class)
	public void test2() throws Exception {
		final boolean[] yielded = {false};
		L.push((LuaJavaCallback) L1 -> {
			System.out.println("Yielding");
			yielded[0] = true;
			return L1.yield(0);
		});
		int status = L.resume(0);
		Assert.assertTrue("Status isn't equal to YIELD", status == Lua.YIELD);
		Assert.assertTrue("Thread failed to yield", yielded[0]);
		Assert.assertTrue("The stack top isn't empty", L.getTop() == 0);
	}

	/**
	 * Thread that yields using coroutine.yield.
	 */
	@Test
	@Library(BaseLib.class)
	public void test3() throws Exception {
		Lua L = this.L;
		L.push(L.getField(L.getGlobal("coroutine"), "yield"));
		int status = L.resume(0);
		Assert.assertTrue("Status isn't equal to YIELD", status == Lua.YIELD);
		Assert.assertTrue("The stack top isn't empty", L.getTop() == 0);
	}

	/**
	 * Yielding in a Lua script.
	 */
	@Test
	@Library(BaseLib.class)
	public void test4() throws Exception {
		Lua L = this.L;
		loadFileAndFunction();
		final int n = 4;
		for (int i = 0; i < n; ++i) {
			int status = L.resume(0);
			if (i < n - 1) {
				Assert.assertTrue("Status isn't equal to YIELD", status == Lua.YIELD);
			} else {
				Assert.assertTrue("Status is not 0", status == 0);
			}
			double v = Lua.toNumber(L.getGlobal("v")).orElse(0D);
			Assert.assertTrue("V isn't equal to " + i, v == i);
		}
	}

	@Test
	@Call(1)
	public void test5() throws Exception {
		Assert.assertTrue("The result isn't true", Boolean.TRUE.equals(L.value(-1)));
	}

	@Test
	@Call(1)
	public void test6() throws Exception {
		Assert.assertTrue("The result isn't true", Boolean.TRUE.equals(L.value(-1)));
	}

	@Test
	@Call(1)
	public void test7() throws Exception {
		Assert.assertTrue("The result isn't true", Boolean.TRUE.equals(L.value(-1)));
	}

	@Test
	@Call(1)
	public void test8() throws Exception {
		Assert.assertTrue("The result isn't true", Boolean.TRUE.equals(L.value(-1)));
	}

	@Test
	@Call(1)
	public void test9() throws Exception {
		Assert.assertTrue("The result isn't true", Boolean.TRUE.equals(L.value(-1)));
	}

	@Test
	@Library(None.class)
	public void test10() throws Exception {
		Lua L = this.L;
		loadFileAndFunction();
		L.push((LuaJavaCallback) L1 -> L1.yield(0));
		L.push((LuaJavaCallback) L1 -> {
			throw new RuntimeException("spong");
		});
		boolean start = true;
		int status;
		int n = 0;
		int k = 0;
		while (true) {
			int nargs;
			if (start) {
				nargs = 2;
				start = false;
			} else {
				nargs = 1;
				L.pushNumber(n);
			}
			++n;
			try {
				status = L.resume(nargs);
				if (status != Lua.YIELD)
					break;
			} catch (RuntimeException e) {
				++k;
				Assert.assertTrue("Error message doesn't contain 'spong'", e.getMessage().contains("spong"));
			}
		}
		if (status != 0)
			System.out.println(L.value(-1));
		Assert.assertTrue("Status is not 0", status == 0);
		Assert.assertTrue("k is not 4", k == 4);
		Assert.assertTrue("first return value isn't 16", Lua.toNumber(L.value(-2)).orElse(0D) == 16);
		Assert.assertTrue("second return value isn't 20", Lua.toNumber(L.value(-1)).orElse(0D) == 20);
	}

	@Test
	@Library(None.class)
	public void test11() throws Exception {
		Lua co = L.newThread();
		BaseLib.open(co);
		co.loadString("error('test11 error ' .. ...)", "@test11");
		co.pushString("foo");
		int status = co.resume(1);
		Assert.assertTrue("Status is not ERRRUN", status == Lua.ERRRUN);
		Object o = co.value(-1);
		Assert.assertTrue("Top value off the stack isn't a String", o instanceof String);
		String s = (String) o;
		Assert.assertTrue("Result contains 'test11 error'", s.contains("test11 error"));
		System.out.println("Expected error: " + co.value(-1));
	}
}
