package me.jezza.lava;

import me.jezza.lava.annotations.Call;
import me.jezza.lava.annotations.Name;
import org.junit.Test;

/**
 * @author Jezza
 */
public final class AdvancedTest extends AbstractTest {
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
	protected void call(Lua L, String file, String name, int n) {
		loadFile(L, name);
		int status = L.pcall(0, 0, Lua.ADD_STACK_TRACE);
		if (status != 0)
			System.out.println(L.toString(L.value(-1)));
	}

	@Test
	@Call
	@Name("attrib")
	public void testAttrib() throws Exception {
	}

	@Test
	@Call
	@Name("big")
	public void testBig() throws Exception {
	}

	@Test
	@Call
	@Name("calls")
	public void testCalls() throws Exception {
	}

	@Test
	@Call
	@Name("checktable")
	public void testCheckTable() throws Exception {
	}

	@Test
	@Call
	@Name("closure")
	public void testClosure() throws Exception {
	}

	@Test
	@Call
	@Name("constructs")
	public void testConstructs() throws Exception {
	}

	@Test
	@Call
	@Name("events")
	public void testEvents() throws Exception {
	}

	@Test
	@Call
	@Name("gc")
	public void testGC() throws Exception {
	}

	@Test
	@Call
	@Name("literals")
	public void testLiterals() throws Exception {
	}

	@Test
	@Call
	@Name("locals")
	public void testLocals() throws Exception {
	}

	@Test
	@Call
	@Name("nextvar")
	public void testNextVar() throws Exception {
	}

	@Test
	@Call
	@Name("pm")
	public void testPM() throws Exception {
	}

	@Test
	@Call
	@Name("sort")
	public void testSort() throws Exception {
	}

	@Test
	@Call
	@Name("strings")
	public void testStrings() throws Exception {
	}

	@Test
	@Call
	@Name("vararg")
	public void testVarargs() throws Exception {
	}
}
