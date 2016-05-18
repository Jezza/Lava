package me.jezza.lava;

import me.jezza.lava.annotations.SkipSetup;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 tests for the base objects of Lava.
 * Such as LuaFunction, LuaTable, etc
 *
 * @author Jezza
 */
public class ObjectTest extends AbstractTest {

	/**
	 * Tests basic facts about LuaFunction.
	 */
	@Test
	@SkipSetup
	public void testFunction() throws Exception {
		Proto p = new Proto(new Slot[0], new int[0], new Proto[0], 0, 0, false, 0);

		LuaFunction f = new LuaFunction(p, new UpVal[0], new LuaTable());

		// Check that the type is correct, according to the API.
		Assert.assertTrue(Lua.isFunction(f));
		Assert.assertTrue(!Lua.isJavaFunction(f));
		Assert.assertTrue(!Lua.isNil(f));
		Assert.assertTrue(!Lua.isBoolean(f));
		Assert.assertTrue(!Lua.isNumber(f));
		Assert.assertTrue(!Lua.isString(f));
		Assert.assertTrue(!Lua.isTable(f));
		Assert.assertTrue(!Lua.isUserdata(f));

		Assert.assertSame("Proto stored and not returned", p, f.proto());

		LuaTable e = new LuaTable();
		f.env(e);
		Assert.assertSame("Env stored and not returned", e, f.env());
	}

	/**
	 * Tests basic facts about Userdata.
	 */
	@Test
	@SkipSetup
	public void testUserdata() throws Exception {
		Object o = new Object();
		LuaUserdata u = new LuaUserdata(o);
		Assert.assertNotNull(u);
		// Check that the type is correct, according to the API.
		Assert.assertTrue(Lua.isUserdata(u));
		Assert.assertTrue(!Lua.isNil(u));
		Assert.assertTrue(!Lua.isBoolean(u));
		Assert.assertTrue(!Lua.isNumber(u));
		Assert.assertTrue(!Lua.isString(u));
		Assert.assertTrue(!Lua.isTable(u));
		Assert.assertTrue(!Lua.isFunction(u));

		LuaUserdata another = new LuaUserdata(o);
		Assert.assertNotNull(another);
	}

	/**
	 * Tests storage facilities of Userdata.
	 */
	@Test
	@SkipSetup
	public void testUserdataStore() throws Exception {
		Object o = new Object();
		LuaUserdata u = new LuaUserdata(o);
		LuaUserdata another = new LuaUserdata(o);

		Assert.assertSame("Object stored and not returned", u.userdata(), o);
		Assert.assertSame("Two userdata objects that were set to the same object returned different objects", u.userdata(), another.userdata());

		LuaTable t = new LuaTable();
		u.metatable(t);
		Assert.assertSame("Metatable set and not returned", u.metatable(), t);

		LuaTable e = new LuaTable();
		u.env(e);
		Assert.assertSame("Environment set and not returned", u.env(), e);
	}
}
