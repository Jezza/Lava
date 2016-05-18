package me.jezza.lava;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jezza
 */
public class VMTest extends AbstractTest {

	/**
	 * Tests execution of the OP_LOADBOOL opcode.  This opcode is
	 * generated when the results of a boolean expression are used for its
	 * value (as opposed to inside an "if").  Our test is "return x==nil".
	 * This generates both the skip and non-skip forms.
	 */
	@Test
	public void testVMLoadbool() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestLoadbool");
		L.call(0, 1);
		Object res = L.value(1);
		Boolean b = (Boolean) res;
		Assert.assertTrue("Result is true", b);
		L.rawSet(L.getGlobals(), "x", "foo");
		loadFile(L, "VMTestLoadbool");
		L.call(0, 1);
		res = L.value(-1);
		b = (Boolean) res;
		Assert.assertTrue("Result != false", !b);
	}

	/**
	 * Tests execution of OP_LOADNIL opcode.  This opcode is generated for
	 * assignment sequences like "a,b,c=7".
	 */
	@Test
	public void testVMLoadnil() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestLoadnil");
		L.call(0, 3);
		Object first = L.value(1);
		Double d = (Double) first;
		Assert.assertTrue("First result != 7", d == 7);
		Assert.assertTrue("Second result != nil", L.value(2) == Lua.NIL);
		Assert.assertTrue("Third result != nil", L.value(3) == Lua.NIL);
	}

	/**
	 * Tests execution of OP_ADD opcode.
	 */
	@Test
	public void testVMAdd() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestAdd");
		L.call(0, 1);
		Object res = L.value(1);
		Double d = (Double) res;
		Assert.assertTrue("Result != 18", d == 18);
	}

	/**
	 * Tests execution of OP_SUB opcode.
	 */
	@Test
	public void testVMSub() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestSub");
		L.call(0, 1);
		Object res = L.value(1);
		Double d = (Double) res;
		Assert.assertTrue("Result != 20", d == 20);
	}

	/**
	 * Tests execution of OP_CONCAT opcode.
	 */
	@Test
	public void testVMConcat() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestConcat");
		L.call(0, 1);
		Object res = L.value(1);
		String s = (String) res;
		Assert.assertTrue("Result != foobarbaz", s.equals("foobarbaz"));
	}

	/**
	 * Tests execution of OP_SETTABLE opcode.
	 */
	@Test
	public void testVMSettable() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestSettable");
		L.call(0, 1);
		Object t = L.value(1);
		Double d;
		d = (Double) Lua.rawGet(t, "a");
		Assert.assertTrue("t.a != 1", d == 1);
		d = (Double) Lua.rawGet(t, "b");
		Assert.assertTrue("t.b != 2", d == 2);
		d = (Double) Lua.rawGet(t, "c");
		Assert.assertTrue("t.c != 3", d == 3);
		Assert.assertTrue("t.d != nil", Lua.isNil(Lua.rawGet(t, "d")));
	}

	/**
	 * Tests execution of OP_GETTABLE opcode.
	 */
	@Test
	public void testVMGettable() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestGettable");
		L.call(0, 1);
		Double d = (Double) L.value(1);
		Assert.assertTrue("Result != 28", d == 28);
	}

	/**
	 * Tests execution of OP_SETLIST opcode.
	 * :todo: There are special cases in SETLIST that are currently untested:
	 * When field B is 0 (all elements up to TOS are set);
	 * when field C is 0 (starting index is loaded from next opcode).
	 * The former is presumably generated when a list initialiser has at
	 * least 25,600 (512*50) elements (!).  The latter is generated when an
	 * expression like "{...}" is used.
	 */
	@Test
	public void testVMSetlist() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestSetlist");
		L.call(0, 1);
		Double d = (Double) L.value(1);
		Assert.assertTrue("Result != 32", d == 32);
	}

	/**
	 * Tests execution of OP_CALL opcode (when calling Lua Java function).
	 */
	@Test
	public void testVMCall() throws Exception {
		Lua L = this.L;
		// Create a Lua Java function and install it in the global 'f'.
		LuaJavaCallback callback = L1 -> {
			double d = Lua.toNumber(L.value(1)).orElse(0D);
			L.pushNumber(d * 3);
			return 1;
		};
		L.rawSet(L.getGlobals(), "f", callback);
		loadFile(L, "VMTestCall");
		L.call(0, 1);
		Assert.assertTrue("Result != 22", Lua.toNumber(L.value(1)).orElse(0D) == 22);
	}

	/**
	 * Tests execution of OP_CLOSURE opcode.  Generated when functions
	 * are defined.
	 */
	@Test
	public void testVMClosure() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestClosure");
		L.call(0, 1);
		L.value(1);
		L.push("bar");
		L.call(1, 1);
		Assert.assertTrue("Result != foobar", "foobar".equals(L.value(-1)));
	}

	/**
	 * Tests execution of OP_CALL for calling Lua.
	 */
	@Test
	public void testVMCall1() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestCall1");
		L.call(0, 1);
		L.value(1);
		Assert.assertTrue("Result != fxfy", "fxfy".equals(L.value(-1)));
	}

	/**
	 * Tests execution of OP_JMP opcode.
	 */
	@Test
	public void testVMJmp() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestJmp");
		L.call(0, 1);
		Object o = L.value(1);
		Assert.assertTrue("Result != 15", o.equals(15D));
	}

	/**
	 * Tests execution of OP_JMP opcode some more.  The function is taken
	 * from the "3x+1" problem.  It computes elements from Sloane's
	 * sequence A006577
	 * (see http://www.research.att.com/~njas/sequences/A006577 ).
	 */
	@Test
	public void testVMJmp1() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestJmp1");
		L.call(0, 1);
		Object o = L.value(1);
		Assert.assertTrue("Result != 111", o.equals(111D));
	}

	/**
	 * Tests execution of Upvalues.  This tests OP_GETUPVAL, OP_SETUPVAL,
	 * that OP_CLOSURE creates UpVals, and that OP_RET closes UpVals.
	 */
	@Test
	public void testVMUpval() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestUpval");
		L.call(0, 1);
		LuaFunction f = (LuaFunction) L.value(1);
		L.push(f);
		L.call(0, 0);
		L.push(f);
		L.call(0, 0);
		L.push(f);
		L.call(0, 1);
		Object o = L.value(-1);
		Assert.assertTrue("Result != 3", o.equals(3D));
	}

	/**
	 * Tests execution of OP_LE opcode.
	 */
	@Test
	public void testVMLe() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestLe");
		L.call(0, 1);
		Object o = L.value(1);
		Assert.assertTrue("Result != 11", o.equals(11D));
	}

	/**
	 * Tests execution of OP_TEST opcode.
	 */
	@Test
	public void testVMTest() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestTest");
		L.call(0, 1);
		Object o = L.value(-1);
		Assert.assertTrue("Result != 2", o.equals(2D));
		L.rawSet(L.getGlobals(), "x", Boolean.FALSE);
		loadFile(L, "VMTestTest");
		L.call(0, 1);
		o = L.value(-1);
		Assert.assertTrue("Result != 2", o.equals(2D));
		L.rawSet(L.getGlobals(), "x", Boolean.TRUE);
		loadFile(L, "VMTestTest");
		L.call(0, 1);
		o = L.value(-1);
		Assert.assertTrue("Result != 1", o.equals(1D));
	}

	/**
	 * Tests execution of OP_TESTSET opcode.
	 */
	@Test
	public void testVMTestset() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestTestset");
		L.rawSet(L.getGlobals(), "y", 7D);
		L.call(0, 2);
		Object o = L.value(-2);
		Object p = L.value(-1);
		Assert.assertTrue("x or y != 7", o.equals(7D));
		Assert.assertTrue("x and y != nil", Lua.isNil(p));
		L.rawSet(L.getGlobals(), "x", Boolean.TRUE);
		loadFile(L, "VMTestTestset");
		L.call(0, 2);
		o = L.value(-2);
		p = L.value(-1);
		Assert.assertTrue("x or y != true", o.equals(Boolean.TRUE));
		Assert.assertTrue("x and y != 7", p.equals(7D));
	}

	/**
	 * Tests execution of OP_UNM opcode.
	 */
	@Test
	public void testVMUnm() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestUnm");
		L.call(0, 2);
		Object o = L.value(1);
		Object p = L.value(2);
		Assert.assertTrue("First result != 7", o.equals(7D));
		Assert.assertTrue("Second result != -7", p.equals(-7D));
	}

	/**
	 * Tests execution of OP_NOT opcode.
	 */
	@Test
	public void testVMNot() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestNot");
		L.call(0, 4);
		Assert.assertTrue("First result != true", L.value(1).equals(Boolean.TRUE));
		Assert.assertTrue("Second result != true", L.value(2).equals(Boolean.TRUE));
		Assert.assertTrue("Third result != false", L.value(3).equals(Boolean.FALSE));
		Assert.assertTrue("Fourth result != false", L.value(4).equals(Boolean.FALSE));
	}

	/**
	 * Tests execution of OP_LEN opcode.
	 */
	@Test
	public void testVMLen() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestLen");
		L.call(0, 4);
		Assert.assertTrue("First result != 0", L.value(1).equals(0D));
		Assert.assertTrue("Second result != 0", L.value(2).equals(0D));
		Assert.assertTrue("Third result != 3", L.value(3).equals(3D));
		Assert.assertTrue("Fourth result != 6", L.value(4).equals(6D));
	}

	/**
	 * Tests execution of OP_CLOSE opcode.
	 */
	@Test
	public void testVMClose() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestClose");
		L.call(0, 2);
		Object f = L.value(1);
		Object g = L.value(2);
		// Check that f and g have different upvalues by calling them
		// different numbers of times and checking their results.
		for (int i = 0; i < 3; ++i) {
			L.push(f);
			L.call(0, 0);
		}
		L.push(g);
		L.call(0, 1);
		Assert.assertTrue("g's result != 1", L.value(-1).equals(1D));
		L.push(f);
		L.call(0, 1);
		Assert.assertTrue("f's result != 4", L.value(-1).equals(4D));
	}

	/**
	 * Tests execution of OP_VARARG opcode.
	 */
	@Test
	public void testVMVararg() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestVararg");
		L.call(0, 0);  // side-effect, defines global 'f'
		L.push(Lua.rawGet(L.getGlobals(), "f"));
		int narg = 7;
		for (int i = 0; i < narg; ++i) {
			L.push(Integer.toString(i));
		}
		L.call(narg, 3);
		Assert.assertTrue("First result != '0'", "0".equals(L.value(-3)));
		Assert.assertTrue("Second result != '2'", "2".equals(L.value(-2)));
		Assert.assertTrue("Third result != '3'", "3".equals(L.value(-1)));

		// Same, but call f from Lua this time.
		loadFile(L, "VMTestVararg1");
		L.call(0, 3);
		Assert.assertTrue("First result != one", "one".equals(L.value(-3)));
		Assert.assertTrue("Second result != three", "three".equals(L.value(-2)));
		Assert.assertTrue("Third result != four", "four".equals(L.value(-1)));
	}

	/**
	 * Tests execution of OP_SELF opcode.  Generated when functions
	 * are called using the colon syntax (t:f()).
	 */
	@Test
	public void testVMSelf() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestSelf");
		L.call(0, 1);
		Assert.assertTrue("Result != bar", "bar".equals(L.value(-1)));
	}

	/**
	 * Tests execution of OP_TAILCALL opcode.  Both when b==0 and when b > 0.
	 */
	@Test
	public void testVMTailcall() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestTailcall");
		L.call(0, 1);
		Assert.assertTrue("Result != 3", Double.valueOf(3).equals(L.value(-1)));
	}

	/**
	 * Tests conversion of numbers to string.  Pretty weak, but
	 * illustrates a difference between PUC-Rio Lua 5.1 and Jill.
	 */
	@Test
	public void testVMConvert() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestConvert");
		L.call(0, 2);
		Assert.assertTrue("First result != false", Boolean.FALSE.equals(L.value(-2)));
		System.out.println("VMTestConvert conversion: " + L.value(-1));
	}

	/**
	 * Tests conversion of strings to numbers.
	 */
	@Test
	public void testVMConvert1() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestConvert1");
		L.call(0, 1);
		Assert.assertTrue("Result != 0.375", Double.valueOf(0.375D).equals(L.value(-1)));
	}

	/**
	 * Tests conversion of a hexadecimal string to number.
	 */
	@Test
	public void testVMConvert2() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestConvert2");
		L.call(0, 1);
		Assert.assertTrue("Result != 255", Double.valueOf(255D).equals(L.value(-1)));
	}

	/**
	 * Tests for loop.
	 */
	@Test
	public void testVMFor() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestFor");
		L.call(0, 1);
		Assert.assertTrue("Result != 55", Double.valueOf(55D).equals(L.value(-1)));
	}

	/**
	 * Tests generator based for loop.
	 */
	@Test
	public void testVMFor1() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestFor1");
		L.call(0, 1);
		Assert.assertTrue("Result != 55", Double.valueOf(55D).equals(L.value(-1)));
	}

	/**
	 * Tests that OP_RET pads out correctly with nils.
	 */
	@Test
	public void testVMRet() throws Exception {
		Lua L = this.L;
		loadFile(L, "VMTestRet");
		L.call(0, 3);
		for (int i = 1; i <= 3; ++i)
			Assert.assertTrue("Result " + i + " != nil", Lua.isNil(L.value(i)));
	}

	/**
	 * Tests that the rule of Conway's life is correctly implemented.
	 */
	@Test
	public void testLifeLua() throws Exception {
		Lua L = this.L;
		call(L, "VMTest.lua", "testliferule", 1);
		double v = Lua.toNumber(L.value(-1)).orElse(0D);
		System.out.println(v);
		Assert.assertTrue("Result != 16480", v == 16480);
	}

	@Test
	public void testLifeLuc() throws Exception {
		Lua L = this.L;
		call(L, "VMTest.luc", "testliferule", 1);
		double v = Lua.toNumber(L.value(-1)).orElse(0D);
		System.out.println(v);
		Assert.assertTrue("Result != 16480", v == 16480);
	}

	@Test
	public void testindexnil() throws Exception {
		Lua L = this.L;
		call(L, "VMTest.lua", "testindexnil", 1);
		Assert.assertTrue("Result != nil", Lua.isNil(L.value(-1)));
	}

	@Test
	public void testlentable() throws Exception {
		Lua L = this.L;
		call(L, "VMTest.lua", "testlentable", 1);
		Assert.assertTrue("Result != 9", Lua.toNumber(L.value(-1)).orElse(0D) == 9);
	}

	@Test
	public void testrehash() throws Exception {
		Lua L = this.L;
		call(L, "VMTest.lua", "testrehash", 1);
		Assert.assertTrue("Result != true", L.value(-1).equals(Boolean.TRUE));
	}
}
