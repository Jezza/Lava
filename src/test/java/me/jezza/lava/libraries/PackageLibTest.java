package me.jezza.lava.libraries;

import me.jezza.lava.*;
import me.jezza.lava.annotations.Expected;
import me.jezza.lava.annotations.Library;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for {@link PackageLib}.
 * <p>
 * Auxiliary files:
 * PackageLibTest.lua - test functions.
 *
 * @author Jezza
 */
public final class PackageLibTest extends AbstractTest {
	private static final String[] DEFINED = {
			"seeall", "loaders", "path", "loaded", "preload"
	};

	private static final String[] DEFINED_GLOBALS = {
			"module", "require"
	};

	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
		PackageLib.open(L);
		MathLib.open(L);
		OSLib.open(L);
		StringLib.open(L);
		TableLib.open(L);
	}

	@Override
	protected void call(String name, int n) {
		call("PackageLibTest", name, n);
	}

	@Test
	@Library(PackageLib.class)
	public void testLib() throws Exception {
		Lua L = this.L;

		// Check the globals for the package table.
		Object lib = L.getGlobal("package");
		Assert.assertTrue("Package table isn't defined.", Lua.isTable(lib));

		// Loop through expected fields within the Package table, if this fails, then either the array needs to be updated, or something broke.
		for (String name : DEFINED)
			Assert.assertTrue("The entry '" + name + "' doesn't exist within PackageLib.", !Lua.isNil(L.getField(lib, name)));

		// Globals that should be defined because of the PackageLib, if these fail, then something has changed and the tests haven't been altered, or something broke.
		for (String name : DEFINED_GLOBALS)
			Assert.assertTrue("The entry '" + name + "' doesn't exist within the globals table.", !Lua.isNil(L.getGlobal(name)));
	}

	@Test
	@Expected(0)
	public void test1() throws Exception {
	}

	@Test
	@Expected(0)
	public void test2() throws Exception {
	}

	@Test
	@Expected(0)
	public void test3() throws Exception {
	}

	@Test
	@Expected(0)
	public void test4() throws Exception {
	}

	@Test
	@Expected(0)
	public void test5() throws Exception {
	}

	@Test
	@Expected(0)
	public void test6() throws Exception {
	}

	@Test
	@Expected(0)
	public void test7() throws Exception {
	}

	@Test
	@Expected(0)
	public void test8() throws Exception {
	}

	@Test
	@Expected(0)
	public void test9() throws Exception {
	}

	@Test
	@Expected(0)
	public void test10() throws Exception {
	}

	@Test
	@Expected(0)
	public void test11() throws Exception {
	}
}
