package me.jezza.lava;

import me.jezza.lava.annotations.Call;
import me.jezza.lava.annotations.Name;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jezza
 */
public final class AcceptTest extends AbstractTest {
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
		System.out.println("Running: " + name);
		loadFile(L, "accept-basic/" + name);
		L.call(0, 0);
	}

	@Test
	@Call
	@Name("bisect")
	public void testBisect() throws Exception {
	}

	@Test
	@Call
	@Name("cf")
	public void testCF() throws Exception {
	}

	@Test
	@Call
	@Name("factorial")
	public void testFactorial() throws Exception {
	}

	@Test
	@Call
	@Name("fib")
	public void testFib() throws Exception {
	}

	@Test
	@Call
	@Name("fibfor")
	public void testFibfor() throws Exception {
	}

	@Test
	@Call
	@Name("life")
	public void testLife() throws Exception {
	}

	@Test
	@Name("readonly")
	public void testReadOnly() throws Exception {
		System.out.println("Running: readonly");
		loadFile("accept-basic/readonly");
		int status = L.pcall(0, 0, null);
		Assert.assertTrue("Script raised error", status != 0);
		String s = (String) L.value(-1);
		System.out.println(s);
		Assert.assertTrue("error doesn't look plausible", s.contains("redefine"));
	}

	@Test
	@Call
	@Name("sieve")
	public void testSieve() throws Exception {
	}

	@Test
	@Call
	@Name("sort")
	public void testSort() throws Exception {
	}
}
