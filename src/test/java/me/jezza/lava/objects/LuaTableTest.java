package me.jezza.lava.objects;

import java.util.Iterator;

import me.jezza.lava.AbstractTest;
import me.jezza.lava.Lua;
import me.jezza.lava.LuaTable;
import me.jezza.lava.annotations.SkipSetup;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 tests pertaining to the {@link me.jezza.lava.LuaTable}
 *
 * @author Jezza
 */
public class LuaTableTest extends AbstractTest {

	/**
	 * Tests basic facts about LuaTable.
	 */
	@Test
	@SkipSetup
	public void testTable() throws Exception {
		LuaTable table = new LuaTable();

		// Check that the type is correct, according to the API.
		Assert.assertTrue(Lua.isTable(table));
		Assert.assertTrue(!Lua.isNil(table));
		Assert.assertTrue(!Lua.isBoolean(table));
		Assert.assertTrue(!Lua.isNumber(table));
		Assert.assertTrue(!Lua.isString(table));
		Assert.assertTrue(!Lua.isFunction(table));
		Assert.assertTrue(!Lua.isUserdata(table));

		Assert.assertTrue(table != new LuaTable());

		Assert.assertTrue("Table type token isn't defined as a table", Lua.type(table) == Lua.TTABLE);
	}

	/**
	 * Tests Metatable of LuaTable.
	 */
	@Test
	public void testTableMeta() throws Exception {
		Lua L = this.L;
		LuaTable table = new LuaTable();
		LuaTable meta = new LuaTable();

		L.setMetatable(table, meta);
		Assert.assertSame("Metatable stored and not returned", meta, L.getMetatable(table));

		LuaTable another = new LuaTable();
		LuaTable anotherMeta = new LuaTable();

		L.setMetatable(another, anotherMeta);
		Assert.assertTrue("Tables' metatables are the same", L.getMetatable(table) != L.getMetatable(another));
	}

	@Test
	public void testTableInsertion() throws Exception {
		Lua L = this.L;
		LuaTable t = new LuaTable(0, 3);
		L.rawSetI(t, 1, "'1'");
		L.rawSet(t, "key", "value");
		L.rawSetI(t, 2, "'2'");
		Iterator<Object> it = t.keys();
		Assert.assertTrue("Iterator should have more objects", it.hasNext());
		Assert.assertTrue("Iterator order doesn't go through the array first", "'1'".equals(Lua.rawGet(t, it.next())));
		Assert.assertTrue("Iterator should have more objects", it.hasNext());
		Assert.assertTrue("Iterator order doesn't go through the array first", "'2'".equals(Lua.rawGet(t, it.next())));
		Assert.assertTrue("Iterator should have more objects", it.hasNext());
		Assert.assertTrue("Iterator order doesn't go through the array first", "value".equals(Lua.rawGet(t, it.next())));
	}

	@Test
	public void testTableLength() throws Exception {
		Lua L = this.L;
		LuaTable t = new LuaTable(0, 3);
		Assert.assertTrue("#t != 0", Lua.objLen(t) == 0);
		L.rawSetI(t, 1, "'1'");
		Assert.assertTrue("#t != 1", Lua.objLen(t) == 1);
		L.rawSet(t, "key", "value");
		Assert.assertTrue("#t != 1", Lua.objLen(t) == 1);
	}
}
