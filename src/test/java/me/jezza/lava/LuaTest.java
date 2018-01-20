package me.jezza.lava;

import me.jezza.lava.annotations.Library;
import me.jezza.lava.annotations.Library.None;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jezza
 */
public class LuaTest extends AbstractTest {

	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
		MathLib.open(L);
		OSLib.open(L);
		StringLib.open(L);
		TableLib.open(L);
	}

	/**
	 * Helper used by testLua1.
	 */
	private void simpleScript(String filename) {
		Lua L = new Lua();
		loadFile(L, filename);
		int top = L.getTop();
		Assert.assertTrue("TOS != 1", 1 == top);
		L.call(0, 0);
		top = L.getTop();
		Assert.assertTrue("TOS != 0", 0 == top);
		loadFile(L, filename);
		L.call(0, 1);
		top = L.getTop();
		Assert.assertTrue("More than one result", 1 == top);
		Assert.assertTrue("Result isn't 99.0", (Double) L.value(1) == 99.0);
	}

	/**
	 * Test loading and execution of a couple of simple files.
	 * LoaderTest0.luc and LoaderTest3.luc are compiled from the same Lua source, but '0' is
	 * compiled on a big-endian architecture, and '3' is compiled on a
	 * little-endian architecture.
	 */
	@Test
	@Library(None.class)
	public void testLua1() throws Exception {
		simpleScript("LoaderTest0");
		simpleScript("LoaderTest3");
	}

	/**
	 * Tests that a Lua Java function can be called.
	 */
	@Test
	@Library(None.class)
	public void testLua2() throws Exception {
		Object[] v = new Object[1];
		Object MAGIC = new Object();
		L.push((LuaJavaCallback) L1 -> {
			v[0] = MAGIC;
			return 0;
		});
		L.call(0, 0);
		Assert.assertTrue("Callback wasn't called", v[0] == MAGIC);
	}

	/**
	 * Tests that the Lua script in the plan can be executed.
	 */
	@Test
	@Library(None.class)
	public void testLua3() throws Exception {
		loadFile("LuaTest0");
		L.call(0, 1);
		System.out.println(L.value(1));
		Assert.assertTrue("Result isn't 7foo", "7foo".equals(L.value(1)));
	}

	/**
	 * Test that we can set a table entry to nil.  Because the API is not
	 * yet complete this uses {@link Lua#setGlobal} instead of the more obvious
	 * Lua.setTable.  This is indicative for Ravenbrook job001451.
	 */
	@Test
	@Library(None.class)
	public void testLua4() throws Exception {
		try {
			L.setGlobal("x", Lua.NIL);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError("Failed to set x to nil");
		}
	}

	/**
	 * Test that a corner case of upvalues works.  This is indicative for
	 * Ravenbrook job001457.
	 */
	@Test
	@Library(None.class)
	public void testLua5() throws Exception {
		loadFile("LuaTest5");
		L.call(0, 1);
		Assert.assertTrue("Result wasn't true", Boolean.TRUE.equals(L.value(1)));
	}

	/**
	 * Tests that an error in a hook does not prevent subsequent hooks
	 * from running.
	 */
	@Test
	public void testLua6() throws Exception {
		L.setHook(new LuaTestHook(true), Lua.MASK_COUNT, 100);
		loadFile("speed/fannkuch.lua");
		int status = L.pcall(0, 0, null);
		Assert.assertTrue("Status isn't 0", status != 0);
		Assert.assertTrue("Status is Lua.YIELD", status != Lua.YIELD);
		Assert.assertTrue("Error value isn't a string", L.value(-1) instanceof String);
		String s = (String) L.value(-1);
		Assert.assertTrue("Error message doesn't contain spong", s.contains("spong"));

		LuaTestHook hook = new LuaTestHook(false);
		L.setHook(hook, Lua.MASK_COUNT, 100);
		loadFile("speed/fannkuch.lua");
		status = L.pcall(0, 0, null);
		Assert.assertTrue("Status isn't 0", status == 0);
		Assert.assertTrue("The Hook didn't run many times, something is off.", hook.n > 99);
	}

	private static class LuaTestHook implements Hook {
		private final boolean error;        // used by luaHook
		private int n;        // used by luaHook

		public LuaTestHook(boolean error) {
			this.error = error;
		}

		@Override
		public int luaHook(Lua L, Debug ar) {
			++n;
			if (error)
				throw L.error("spong in hook");
			return 0;
		}
	}

	private static final String INPUT = "-- The Computer Language Benchmarks Game\n-- http://shootout.alioth.debian.org/\n" + "-- contributed by Mike Pall\n" + "\n" + "local function fannkuch(n)\n" + "  local p, q, s, odd, check, maxflips = {}, {}, {}, true, 0, 0\n" + "  for i=1,n do p[i] = i; q[i] = i; s[i] = i end\n" + "  repeat\n" + "    -- Print max. 30 permutations.\n" + "    if check < 30 then\n" + "      if not p[n] then return maxflips end\t-- Catch n = 0, 1, 2.\n" + "      io.write(unpack(p)); io.write(\"\\n\")\n" + "      check = check + 1\n" + "    end\n" + "    -- Copy and flip.\n" + "    local q1 = p[1]\t\t\t\t-- Cache 1st element.\n" + "    if p[n] ~= n and q1 ~= 1 then\t\t-- Avoid useless work.\n" + "      for i=2,n do q[i] = p[i] end\t\t-- Work on a copy.\n" + "      for flips=1,1000000 do\t\t\t-- Flip ...\n" + "\tlocal qq = q[q1]\n" + "\tif qq == 1 then\t\t\t\t-- ... until 1st element is 1.\n" + "\t  if flips > maxflips then maxflips = flips end -- New maximum?\n" + "\t  break\n" + "\tend\n" + "\tq[q1] = q1\n" + "\tif q1 >= 4 then\n" + "\t  local i, j = 2, q1 - 1\n" + "\t  repeat q[i], q[j] = q[j], q[i]; i = i + 1; j = j - 1; until i >= j\n" + "\tend\n" + "\tq1 = qq\n" + "      end\n" + "    end\n" + "    -- Permute.\n" + "    if odd then\n" + "      p[2], p[1] = p[1], p[2]; odd = false\t-- Rotate 1<-2.\n" + "    else\n" + "      p[2], p[3] = p[3], p[2]; odd = true\t-- Rotate 1<-2 and 1<-2<-3.\n" + "      for i=3,n do\n" + "\tlocal sx = s[i]\n" + "\tif sx ~= 1 then s[i] = sx-1; break end\n" + "\tif i == n then return maxflips end\t-- Out of permutations.\n" + "\ts[i] = i\n" + "\t-- Rotate 1<-...<-i+1.\n" + "\tlocal t = p[1]; for j=1,i do p[j] = p[j+1] end; p[i+1] = t\n" + "      end\n" + "    end\n" + "  until false\n" + "end\n" + "\n" + "local n = tonumber(arg and arg[1]) or 1\n" + "io.write(\"Pfannkuchen(\", n, \") = \", fannkuch(n), \"\\n\")";

	@Test
	public void testLua7() throws Exception {
		long start = System.nanoTime();
		Lua L = this.L;

		int i = L.loadString(INPUT, "fannkuch.test");
		int status = L.pcall(0, 0, null);
		Assert.assertTrue("status not 0", status != 0);

		long finish = System.nanoTime();
		System.out.println(finish - start);
	}
}
