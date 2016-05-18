package me.jezza.lava.libraries;

import me.jezza.lava.AbstractTest;
import me.jezza.lava.BaseLib;
import me.jezza.lava.Lua;
import me.jezza.lava.annotations.Expected;
import me.jezza.lava.annotations.Library;
import me.jezza.lava.annotations.Name;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for {@link BaseLib}.
 * <p>
 * Auxiliary files:
 * BaseLibTestLoadfile.lua - returns 99
 * BaseLibTest.lua - contains functions that test each of base library functions. It is important (for testing "error") that this file is not stripped if it is loaded in binary form.
 * <p>
 * :todo: test radix conversion for tonumber.
 * :todo: test unpack with non-default arguments.
 * :todo: test rawequal for things with metamethods.
 * :todo: test rawget for tables with metamethods.
 * :todo: test rawset for tables with metamethods.
 * :todo: (when string library is available) test the strings returned by assert.
 *
 * @author Jezza
 */
public final class BaseLibTest extends AbstractTest {
	private static final String[] DEFINED = {
			"_VERSION", "_G", "ipairs", "pairs", "print",
			"rawequal", "rawget", "rawset", "select",
			"tonumber", "tostring", "type", "unpack"
	};

	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
	}

	@Override
	protected void call(String name, int n) {
		call("BaseLibTest", name, n);
	}

	@Test
	@Library(BaseLib.class)
	public void testLib() throws Exception {
		Lua L = this.L;

		// Loop through expected global fields, if this fails, then either the array needs to be updated, or something broke.
		for (String name : DEFINED)
			Assert.assertTrue("The entry '" + name + "' doesn't exist within the global table.", !Lua.isNil(L.getGlobal(name)));
	}

	@Test
	@Expected(0)
	@Name("testprint")
	public void testPrint() throws Exception {
	}

	@Test
	@Expected(5)
	@Name("testtostring")
	public void testToString() throws Exception {
	}

	@Test
//	@Expected(5)
	@Name("testtonumber")
	public void testToNumber() throws Exception {
		expected(5);
	}

	@Test
	@Expected(6)
	@Name("testtype")
	public void testType() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testselect")
	public void testSelect() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testunpack")
	public void testUnpack() throws Exception {
	}

	@Test
	@Expected(4)
	@Name("testpairs")
	public void testPairs() throws Exception {
	}

	@Test
	@Expected(4)
	@Name("testnext")
	public void testNext() throws Exception {
	}

	@Test
	@Expected(4)
	@Name("testipairs")
	public void testIpairs() throws Exception {
	}

	@Test
	@Expected(7)
	@Name("testrawequal")
	public void testRawequal() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testrawget")
	public void testRawget() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testrawset")
	public void testRawset() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testgetfenv")
	public void testGetfenv() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testsetfenv")
	public void testSetfenv() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testpcall")
	public void testPcall() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testerror")
	public void testError() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetatable")
	public void testMetatable() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("test__metatable")
	public void testMetatable2() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("test__tostring")
	public void testToString2() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testcollectgarbage")
	public void testCollectGarbage() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testassert")
	public void testAssert() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testloadstring")
	public void testLoadString() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testloadfile")
	public void testLoadFile() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testload")
	public void testLoad() throws Exception {
	}

	@Test
	@Expected(1)
	@Name("testdofile")
	public void testDoFile() throws Exception {
	}

	@Test
	public void testVersion() throws Exception {
		Object o = L.getGlobal("_VERSION");
		Assert.assertTrue("_VERSION doesn't exist.", o != null);
		Assert.assertTrue("_VERSION isn't a string.", Lua.isString(o));
	}

	@Test
	@Expected(2)
	@Name("testerrormore")
	public void testErrormore() throws Exception {
	}

	@Test
	@Expected(4)
	public void testpcall2() throws Exception {
	}

	@Test
	@Expected(2)
	public void testpcall3() throws Exception {
	}

	@Test
	@Expected(1)
	public void testxpcall() throws Exception {
	}

	@Test
	@Expected(1)
	public void testpcall4() throws Exception {
	}

	@Test
	@Expected(1)
	public void testpcall5() throws Exception {
	}

	@Test
	@Expected(1)
	public void testunpackbig() throws Exception {
	}

	@Test
	@Expected(1)
	public void testloaderr() throws Exception {
	}

	@Test
	@Expected(1)
	public void testnanindex() throws Exception {
	}

	@Test
	@Expected(1)
	public void testhexerror() throws Exception {
	}
}
