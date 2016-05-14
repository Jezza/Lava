package me.jezza.lava.libraries;

import me.jezza.lava.AbstractTest;
import me.jezza.lava.BaseLib;
import me.jezza.lava.Lua;
import me.jezza.lava.TableLib;
import me.jezza.lava.annotations.Expected;
import me.jezza.lava.annotations.Library;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for {@link TableLib}.
 * <p>
 * Auxiliary files:
 * TableLibTest.lua - test functions.
 * TableLibTest.luc - compiled .lua file.
 *
 * @author Jezza
 */
public final class TableLibTest extends AbstractTest {
	private static final String[] DEFINED = {
			"concat", "insert", "maxn",
			"remove", "sort"
	};

	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
		TableLib.open(L);
	}

	@Override
	protected void call(String name, int n) {
		call("TableLibTest", name, n);
	}

	@Test
	@Library(TableLib.class)
	public void testLib() throws Exception {
		Lua L = this.L;

		// Check the globals for the table table.
		Object lib = L.getGlobal("table");
		// Dat error message...
		Assert.assertTrue("Table table isn't defined.", Lua.isTable(lib));

		// Loop through expected fields within the OS table, if this fails, then either the array needs to be updated, or something broke.
		for (String name : DEFINED)
			Assert.assertTrue("The entry '" + name + "' doesn't exist within TableLib.", !Lua.isNil(L.getField(lib, name)));
	}

	@Test
	@Expected(1)
	public void testconcat() throws Exception {
	}

	@Test
	@Expected(1)
	public void testconcat2() throws Exception {
	}

	@Test
	@Expected(1)
	public void testinsertremove() throws Exception {
	}

	@Test
	@Expected(1)
	public void testmaxn() throws Exception {
	}

	@Test
	@Expected(1)
	public void testsort() throws Exception {
	}
}
