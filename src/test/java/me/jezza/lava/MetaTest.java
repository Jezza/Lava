package me.jezza.lava;

import me.jezza.lava.annotations.Expected;
import me.jezza.lava.annotations.Name;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for metatable functions.
 * <p>
 * Auxiliary files:
 * MetaTest.lua
 *
 * @author Jezza
 */
public class MetaTest extends AbstractTest {
	@Override
	protected void populate(Lua L) {
		BaseLib.open(L);
	}

	@Test
	@Expected(4)
	@Name("testmetaindex0")
	public void testMetaIndex0() throws Exception {
	}

	@Test
	@Expected(4)
	@Name("testmetaindex1")
	public void testMetaIndex1() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetaindex2")
	public void testMetaIndex2() throws Exception {
	}

	@Test
	@Expected(5)
	@Name("testmetanewindex0")
	public void testMetaNewIndex0() throws Exception {
	}

	@Test
	@Expected(4)
	@Name("testmetaindex1")
	public void testMetaNewIndex1() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetaindex2")
	public void testMetaNewIndex2() throws Exception {
	}

	@Test
	@Expected(3)
	@Name("testmetacall")
	public void testMetaCall() throws Exception {
	}

	@Test
	@Expected(6)
	@Name("testmetalt")
	public void testMetaLt() throws Exception {
	}

	@Test
	@Expected(6)
	@Name("testmetale")
	public void testMetaLe() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetaadd")
	public void testMetaAdd() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetasub")
	public void testMetaSub() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetamul")
	public void testMetaMul() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetadiv")
	public void testMetaDiv() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetamod")
	public void testMetaMod() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetapow")
	public void testMetaPow() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetaconcat")
	public void testMetaConcat() throws Exception {
	}

	@Test
	@Expected(2)
	@Name("testmetaunm")
	public void testMetaUnm() throws Exception {
	}

	@Test
	@Expected(9)
	@Name("testmetaconst")
	public void testMetaConst() throws Exception {
	}

	@Test
	@Expected(3)
	@Name("testmetaeq")
	public void testMetaEq() throws Exception {
	}

	@Test
	@Name("testmetalen")
	public void testMetaLen() {
		Lua L = this.L;
		LuaTable mt = new LuaTable();
		L.setMetatable(0D, mt);
		expected(3);
	}
}
