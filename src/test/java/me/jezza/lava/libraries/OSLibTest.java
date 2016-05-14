package me.jezza.lava.libraries;

import me.jezza.lava.AbstractTest;
import me.jezza.lava.BaseLib;
import me.jezza.lava.Lua;
import me.jezza.lava.OSLib;
import me.jezza.lava.annotations.Expected;
import me.jezza.lava.annotations.Library;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for {@link OSLib}.
 * <p>
 * Auxiliary files:
 * OSLibTest.lua - test functions.
 * OSLibTest.luc - compiled .lua file.
 *
 * @author Jezza
 */
public final class OSLibTest extends AbstractTest {
	private static final String[] DEFINED = {
			"clock", "date", "difftime",
			"setlocale", "time"
	};

	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
		OSLib.open(L);
	}

	@Override
	protected void call(String name, int n) {
		call("OSLibTest", name, n);
	}

	@Test
	@Library(OSLib.class)
	public void testLib() throws Exception {
		Lua L = this.L;

		// Check the globals for the os table.
		Object lib = L.getGlobal("os");
		Assert.assertTrue("OS table isn't defined.", Lua.isTable(lib));

		// Loop through expected fields within the OS table, if this fails, then either the array needs to be updated, or something broke.
		for (String name : DEFINED)
			Assert.assertTrue("The entry '" + name + "' doesn't exist within OSLib.", !Lua.isNil(L.getField(lib, name)));
	}

	@Test
	@Expected(2)
	public void testclock() throws Exception {
	}

	@Test
	@Expected(1)
	public void testdate() throws Exception {
	}

	@Test
	@Expected(1)
	public void testtime() throws Exception {
	}
}
