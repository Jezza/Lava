package me.jezza.lava.libraries;

import me.jezza.lava.AbstractTest;
import me.jezza.lava.BaseLib;
import me.jezza.lava.Lua;
import me.jezza.lava.MathLib;
import me.jezza.lava.annotations.Expected;
import me.jezza.lava.annotations.Library;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for {@link MathLib}.
 * <p>
 * Auxiliary files:
 * MathLibTest.lua - test functions.
 *
 * @author Jezza
 */
public final class MathLibTest extends AbstractTest {
	private static final String[] DEFINED = {
			"abs", "ceil", "cos", "deg", "exp", "floor",
			"fmod", "max", "min", "modf", "pow", "rad",
			"random", "randomseed", "sin", "sqrt", "tan",
			"pi", "huge"
	};

	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
		MathLib.open(L);
	}

	@Override
	protected void call(String name, int n) {
		call("MathLibTest", name, n);
	}

	@Test
	@Library(MathLib.class)
	public void testLib() throws Exception {
		Lua L = this.L;

		// Check the globals for the maths table.
		Object lib = L.getGlobal("math");
		Assert.assertTrue("Maths table isn't defined.", Lua.isTable(lib));

		// Loop through expected fields within the Maths table, if this fails, then either the array needs to be updated, or something broke.
		for (String name : DEFINED)
			Assert.assertTrue("The entry '" + name + "' doesn't exist within MathLib.", !Lua.isNil(L.getField(lib, name)));
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

}
