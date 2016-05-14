package me.jezza.lava.libraries;

import me.jezza.lava.AbstractTest;
import me.jezza.lava.BaseLib;
import me.jezza.lava.Lua;
import me.jezza.lava.StringLib;
import me.jezza.lava.annotations.Expected;
import me.jezza.lava.annotations.Library;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for {@link StringLib}.
 * <p>
 * Auxiliary files:
 * StringLibTest.lua - test functions.
 * StringLibTest.luc - compiled .lua file.
 *
 * @author Jezza
 */
public final class StringLibTest extends AbstractTest {
	private static final String[] DEFINED = {
			"byte", "char", "dump", "find",
			"format", "gmatch", "gsub", "len",
			"lower", "match", "rep", "reverse",
			"sub", "upper"
	};

	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
		StringLib.open(L);
	}

	@Override
	protected void call(String name, int n) {
		call("StringLibTest", name, n);
	}

	@Test
	@Library(StringLib.class)
	public void testLib() throws Exception {
		Lua L = this.L;

		// Check the globals for the string table.
		Object lib = L.getGlobal("string");
		Assert.assertTrue("String table isn't defined.", Lua.isTable(lib));

		// Loop through expected fields within the string table, if this fails, then either the array needs to be updated, or something broke.
		for (String name : DEFINED)
			Assert.assertTrue("The entry '" + name + "' doesn't exist within StringLib.", !Lua.isNil(L.getField(lib, name)));
	}

	@Test
	@Expected(2)
	public void testlen() throws Exception {
	}

	@Test
	@Expected(3)
	public void testlower() throws Exception {
	}

	@Test
	@Expected(3)
	public void testrep() throws Exception {
	}

	@Test
	@Expected(3)
	public void testupper() throws Exception {
	}

	@Test
	@Expected(3)
	public void testsub() throws Exception {
	}

	@Test
	@Expected(2)
	public void testmeta() throws Exception {
	}

	@Test
	@Expected(2)
	public void testreverse() throws Exception {
	}

	@Test
	@Expected(5)
	public void testbyte() throws Exception {
	}

	@Test
	@Expected(2)
	public void testchar() throws Exception {
	}

	@Test
	@Expected(6)
	public void testfind() throws Exception {
	}

	@Test
	@Expected(2)
	public void testmatch() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformat() throws Exception {
	}

	@Test
	@Expected(1)
	public void testgsub() throws Exception {
	}

	@Test
	@Expected(1)
	public void testgsub2() throws Exception {
	}

	@Test
	@Expected(1)
	public void testgsub3() throws Exception {
	}

	@Test
	@Expected(1)
	public void testgsub4() throws Exception {
	}

	@Test
	@Expected(1)
	public void testgmatch() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformatmore() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformatx1() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformatx2() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformatx3() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformatx4() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformatx5() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformatx6() throws Exception {
	}

	@Test
	@Expected(1)
	public void testformatx7() throws Exception {
	}

	@Test
	@Expected(1)
	public void testdump() throws Exception {
	}

	@Test
	@Expected(1)
	public void testaritherror() throws Exception {
	}
}