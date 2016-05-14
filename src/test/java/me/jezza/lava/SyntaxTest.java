package me.jezza.lava;

import me.jezza.lava.annotations.Library;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains JUnit4 Tests for the parsing component of Lava.
 *
 * @author Jezza
 */
public class SyntaxTest extends AbstractTest {
	@Override
	protected void populate(Lua L) {
	}

	@Test
	public void testSyntax0() throws Exception {
		System.out.println("Syntax0");
		Lua L = this.L;
		Object o = null;
		try {
			o = Syntax.parser(L, Lua.stringReader(""), "Syntax0");
		} catch (Exception ignored) {
		}
		Assert.assertNotNull("Result is null", o);
	}

	@Test
	public void testSyntax1() throws Exception {
		System.out.println("Syntax1");
		Lua L = this.L;
		L.load(Lua.stringReader(""), "Syntax1");
		L.call(0, 0);
	}

	@Test
	public void testSyntax2() throws Exception {
		System.out.println("Syntax2");
		Lua L = this.L;
		Assert.assertTrue("script 1 okay", 0 == doString(""));
		L.setTop(0);

		Assert.assertTrue("script 2 okay", 0 == doString(" \t"));
		L.setTop(0);

		Assert.assertTrue("script 3 okay", 0 == doString("\n\n"));
		L.setTop(0);

		Assert.assertTrue("script 4 okay", 0 == doString("return 99"));
		Object value = L.value(1);
		Assert.assertTrue("script 4 result test", value instanceof Double && (Double) value == 99.0);
		L.setTop(0);

		Assert.assertTrue("script 5 okay", 0 == doString("  return -99 ;  "));
		value = L.value(1);
		Assert.assertTrue("script 5 result test", value instanceof Double && (Double) value == -99.0);
		L.setTop(0);

		Assert.assertTrue("script 6 okay", 0 == doString("do return 77 end"));
		value = L.value(1);
		Assert.assertTrue("script 6 result test", value instanceof Double && (Double) value == 77.0);
		L.setTop(0);

		Assert.assertTrue("script 7 okay", 0 == doString("repeat do return 77 end until 5"));
		value = L.value(1);
		Assert.assertTrue("script 7 result test", value instanceof Double && (Double) value == 77.0);
		L.setTop(0);

		Assert.assertTrue("script 8 okay", 0 == doString("do local f = 7 ; return f  end"));
		value = L.value(1);
		Assert.assertTrue("script 8 result test", value instanceof Double && (Double) value == 7.0);
		L.setTop(0);

		Assert.assertTrue("script 9 okay", 0 == doString("  return \"This is a String\";  "));
		value = L.value(1);
		Assert.assertTrue("script 9 result test", value instanceof String && value.equals("This is a String"));
		L.setTop(0);

		Assert.assertTrue("script 10 okay", 0 == doString("return true"));
		value = L.value(1);
		Assert.assertTrue("script 10 result test", value instanceof Boolean && (Boolean) value);
		L.setTop(0);

		Assert.assertTrue("script 11 okay", 0 == doString("return false"));
		value = L.value(1);
		Assert.assertTrue("script 11 result test", value instanceof Boolean && !((Boolean) value));
		L.setTop(0);

		Assert.assertTrue("script 12 okay", 0 == doString("return nil"));
		Assert.assertTrue("script 12 result test", L.value(1) == Lua.NIL);
		L.setTop(0);
	}

	/**
	 * Test that function calls are compiled.
	 */
	@Test
	@Library(BaseLib.class)
	public void testSyntax3() {
		System.out.println("Syntax3");
		Assert.assertTrue("script 1 okay", 0 == doString("print'hello'"));
	}

	@Test
	@Library(BaseLib.class)
	public void testSyntax4() {
		System.out.println("Syntax4");
		Lua L = this.L;
		Assert.assertTrue("script 1 okay", 0 == doString(L, "local a, b = 3, 8 ; return a*b"));
		Object value = L.value(1);
		Assert.assertTrue("script 1 result test", value instanceof Double && (Double) value == 24.0);
		L.setTop(0);

		Assert.assertTrue("script 2 okay", 0 == doString(L, "do local a = 6 ; return 8+a end"));
		value = L.value(1);
		Assert.assertTrue("script 2 result test", value instanceof Double && (Double) value == 14.0);
		L.setTop(0);

		Assert.assertTrue("script 3 okay", 0 == doString(L, "local a = 1 ; while a < 5 do print('thing') ; a = a+1 end"));
		L.setTop(0);

		Assert.assertTrue("script 4 okay", 0 == doString(L, "for a = 1, 10 do print('thing') end"));
		L.setTop(0);

		Assert.assertTrue("script 5 okay", 0 == doString(L, "local a = 1 ; a = a+4 ; return a"));
		value = L.value(1);
		Assert.assertTrue("script 5 result test", value instanceof Double && (Double) value == 5.0);
		L.setTop(0);

		Assert.assertTrue("script 6 okay", 0 == doString(L, "local a,b,c,d = 4,'df',7 ; a,d = d,a ; print(a); print(b); print(c); print(d) ; return a"));
		L.setTop(0);
	}

	@Test
	@Library(BaseLib.class)
	public void testSyntax5() {
		System.out.println("Syntax5");
		Lua L = this.L;
		Assert.assertTrue("script 1 okay", 0 == doString(L, "local a = { zong = 42, 100, ['foo'] = 7676 } ; print(a.zong) ; print(a) ; return a['foo']"));
		Object value = L.value(1);
		Assert.assertTrue("script 1 result test", value instanceof Double && (Double) value == 7676.0);
		L.setTop(0);

		Assert.assertTrue("script 2 okay", 0 == doString(L, "local foo; foo = function (a) return a end"));
		L.setTop(0);

		Assert.assertTrue("script 3 okay", 0 == doString(L, "local foo = function (a) return a*a end ; return foo"));
		L.setTop(0);

		Assert.assertTrue("script 4 okay", 0 == doString(L, "local foo = function (a) return a*a end ; return foo(4)"));
		value = L.value(1);
		Assert.assertTrue("script 4 result test", value instanceof Double && (Double) value == 16.0);
		L.setTop(0);

		Assert.assertTrue("script 5 okay", 0 == doString(L, "return function (a, b, ...) local f = function (a) return a end ; local c = a*b ; return f(c),... end"));
		L.setTop(0);

		Assert.assertTrue("script 6 okay", 0 == doString(L, "local foo = function (a, b, ...) local c = a*b ; return c,... end"));
		L.setTop(0);

		Assert.assertTrue("script 7 okay", 0 == doString(L, "local foo = function (a) return a*a end ; return foo(foo(4))"));
		value = L.value(1);
		Assert.assertTrue("script 7 result test", value instanceof Double && (Double) value == 256.0);
		L.setTop(0);
	}

	@Test
	@Library(BaseLib.class)
	public void testSyntax6() {
		System.out.println("Syntax6");
		Lua L = this.L;

		L.setTop(0);
		loadFile(L, "marktest2");
		//describe_stack(L) ;
		Assert.assertTrue(L.value(1) instanceof LuaFunction);

		L.setTop(0);
		loadFile(L, "MetaTest");
		Assert.assertTrue(L.value(1) instanceof LuaFunction);

		L.setTop(0);
		loadFile(L, "ChunkSpy");
		Assert.assertTrue(L.value(1) instanceof LuaFunction);
	}

	@Test
	@Library(BaseLib.class)
	public void testSyntax7() {
		System.out.println("Syntax6");
		Lua L = this.L;

		Assert.assertTrue("if 1 okay", 0 == doString(L, "local a, b = 2, 5 ; if a+3 == b then return a*b-(b/a) else return false end"));
		Object value = L.value(1);
		Assert.assertTrue("if 1 result test", value instanceof Double && (Double) value == 7.5);
		L.setTop(0);

		Assert.assertTrue("if 2 okay", 0 == doString(L, "local a,b,c=true,false,nil if b or c then return -2 elseif a and not b then return 42.0 else return -1 end"));
		value = L.value(1);
		Assert.assertTrue("if 2 result test", value instanceof Double && (Double) value == 42.0);
		L.setTop(0);

		Assert.assertTrue("if 3 okay", 0 == doString(L, "local a = 1 local b if a then b = a*a else b = a+a end if b == 1 then return true else return nil end"));
		value = L.value(1);
		Assert.assertTrue("if 3 result test", value instanceof Boolean && (Boolean) value);
		L.setTop(0);

		Assert.assertTrue("if 4 okay", 0 == doString(L, "local a,b,c,d,e,f = false, true, 7, nil, nil, true ;if (a or (b and c) and (d or not e) and f) then return 'succeed' else return nil end"));
		value = L.value(1);
		Assert.assertTrue("if 4 result test", value instanceof String && "succeed".equals(value));
		L.setTop(0);
	}

	@Test
	@Library(BaseLib.class)
	public void testSyntax8() {
		System.out.println("Syntax8");
		Lua L = this.L;

		L.setTop(0);
		Assert.assertTrue("table 1 okay", 0 == doString(L, "local t = {} ; t.foo = 45 ; t[1] = 17 ; t[3] = 1 ; t[1] = 'laa' ; return t"));
		Object res = L.value(1);
		Assert.assertTrue("table 1 result is LuaTable", res instanceof LuaTable);
		Assert.assertTrue("table 1, t.foo == 45", doubleEqual(L.getTable(res, "foo"), 45));
		Assert.assertTrue("table 1, t[3] == 1", doubleEqual(L.getTable(res, 3.0), 1.0));
		Assert.assertTrue("table 1, t[1] == 'laa'", "laa".equals(L.getTable(res, 1.0)));
		Assert.assertTrue("table 1, t.laa == nil", Lua.isNil(L.getTable(res, "laa")));

		L.setTop(0);
		Assert.assertTrue("table 2 okay", 0 == doString(L, "local t = {} ; t.foo = 45 ; t[1] = 17 ; t[2], t[3] = 2, 1 ; t[1] = 'laa' ; t['foo'] = t t[3] = t[3] + #t; return t"));
		res = L.value(1);
		Assert.assertTrue("table 2 result is LuaTable", res instanceof LuaTable);
		Assert.assertTrue("table 2, t[3] == 4", doubleEqual(L.getTable(res, 3.0), 4.0));
		Assert.assertTrue("table 2, t[1] == 'laa'", "laa".equals(L.getTable(res, 1.0)));

		L.setTop(0);
	}

	private boolean doubleEqual(Object o, double d) {
		return o instanceof Double && (Double) o == d;
	}

	@Test
	@Library(BaseLib.class)
	public void testSyntax9() {
		System.out.println("Syntax9");
		Lua L = this.L;

		L.setTop(0);
		Assert.assertTrue("loop 1 okay", 0 == doString(L, "local a = 0 ; for i = 1, 10 do if i == 7 then break end ; a = a+10 end ; return a"));
		Object res = L.value(1);
		Assert.assertTrue("loop 1 result test", res instanceof Double && (Double) L.value(1) == 60.0);

		L.setTop(0);
		Assert.assertTrue("loop 2 okay", 0 == doString(L, "local a,i = 0,100 ; repeat if i == 7 then break end ; a,i = a+10,i-1 until i == 0 ; return a"));
		res = L.value(1);
		Assert.assertTrue("loop 2 result test", res instanceof Double && (Double) L.value(1) == 930.0);

		L.setTop(0);
		Assert.assertTrue("loop 3 okay", 0 == doString(L, "local a,i = 1,1 ; while i < 100 do i=i*1.8213 local z = 4 ; repeat a = a + 1/256 ; z = z-1 ; if z == 2 then break end until z <= 0;  a = a+a end ; return a ;"));
		res = L.value(1);
		// does 8 outer loops and 2 inner ones... 256 +255/64
		Assert.assertTrue("loop 3 result test", res instanceof Double && (Double) L.value(1) == 259.984375);

		L.setTop(0);
	}

	/**
	 * For historical reasons, this no longer tests the Syntax module
	 * (compiler), but detects a bug in the VM.
	 */
	@Test
	@Library(BaseLib.class)
	public void testSyntax10() {
		System.out.println("Syntax10");
		Lua L = this.L;

		loadFile("SyntaxTest10.luc");
		L.call(0, 1);
		Assert.assertTrue("closures 1 result test", L.value(1) instanceof LuaTable);

		L.setTop(0);
	}
}
