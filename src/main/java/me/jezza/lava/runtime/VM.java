package me.jezza.lava.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Field;

import me.jezza.lava.Bypass;

/**
 * @author Jezza
 */
public final class VM {
	static final class OpCodes {
		static final byte OP_MOVE = 0;
		static final byte OP_LOADK = 1;
		static final byte OP_LOADBOOL = 2;
		static final byte OP_LOADNIL = 3;
		static final byte OP_GETUPVAL = 4;
		static final byte OP_GETGLOBAL = 5;
		static final byte OP_GETTABLE = 6;
		static final byte OP_SETGLOBAL = 7;
		static final byte OP_SETUPVAL = 8;
		static final byte OP_SETTABLE = 9;
		static final byte OP_NEWTABLE = 10;
		static final byte OP_SELF = 11;
		static final byte OP_ADD = 12;
		static final byte OP_SUB = 13;
		static final byte OP_MUL = 14;
		static final byte OP_DIV = 15;
		static final byte OP_MOD = 16;
		static final byte OP_POW = 17;
		static final byte OP_UNM = 18;
		static final byte OP_NOT = 19;
		static final byte OP_LEN = 20;
		static final byte OP_CONCAT = 21;
		// NO
		static final byte OP_JMP = 22;
		static final byte OP_EQ = 23;
		static final byte OP_LT = 24;
		static final byte OP_LE = 25;
		static final byte OP_TEST = 26;
		static final byte OP_TESTSET = 27;
		static final byte OP_CALL = 28;
		static final byte OP_TAILCALL = 29;
		// NO
		static final byte OP_RET = 30;
		static final byte OP_FORLOOP = 31;
		static final byte OP_FORPREP = 32;
		static final byte OP_TFORLOOP = 33;
		static final byte OP_SETLIST = 34;
		static final byte OP_CLOSE = 35;
		static final byte OP_CLOSURE = 36;
		static final byte OP_VARARG = 37;
	}

	static class CS extends MutableCallSite {
		static final MethodHandle FALLBACK, TEST;
		static final MethodHandle[] INSTR_TABLE;

		static {
			Lookup lookup = Bypass.LOOKUP;
			try {
				FALLBACK = lookup.findVirtual(CS.class, "fallback", methodType(void.class, VM.class, int.class));
				TEST = lookup.findStatic(CS.class, "test", methodType(boolean.class, VM.class, int.class, int.class));
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new AssertionError(e);
			}
			Field[] fields = OpCodes.class.getDeclaredFields();
			MethodHandle[] table = new MethodHandle[fields.length];
			for (int i = 0, l = fields.length; i < l; i++) {
				String name = fields[i].getName().substring(3).toLowerCase();
				try {
					table[i] = lookup.findVirtual(VM.class, name, methodType(void.class));
				} catch (NoSuchMethodException | IllegalAccessException e) {
					throw new AssertionError(e);
				}
			}
			INSTR_TABLE = table;
		}

		private final String debug;

		public CS(String debug) {
			super(methodType(void.class, VM.class, int.class));
			this.debug = debug;
			setTarget(FALLBACK.bindTo(this));
		}

		private void fallback(VM interpreter, int ops) throws Throwable {
			MethodHandle dispatch = INSTR_TABLE[ops];

			MethodHandle test = MethodHandles.insertArguments(TEST, 2, ops);
			MethodHandle target = MethodHandles.dropArguments(dispatch, 1, int.class);
			MethodHandle guard = MethodHandles.guardWithTest(test, target, getTarget());
			setTarget(guard);

			INSTR_TABLE[ops].invokeExact(interpreter);
		}

		private static boolean test(VM unused, int ops, int expected) {
			return ops == expected;
		}
	}

	private static MethodHandle dispatch(String debug) {
		return new CS(debug).dynamicInvoker();
	}

	private static final MethodHandle CORE_MH = dispatch("CORE");

	public static void main(String[] args) throws Throwable {
	}

	boolean end;
	int result;
	int pc;
//	final int[] instrs;
	int top;
	Slot[] stack;
//	final int[] locals;

	static class Slot {
	}

	public VM() {
	}

	private void move() {
	}

	private void loadk() {
	}

	private void loadbool() {
	}

	private void loadnil() {
	}

	private void getupval() {
	}

	private void getglobal() {
	}

	private void gettable() {
	}

	private void setglobal() {
	}

	private void setupval() {
	}

	private void settable() {
	}

	private void newtable() {
	}

	private void self() {
	}

	private void add() {
	}

	private void sub() {
	}

	private void mul() {
	}

	private void div() {
	}

	private void mod() {
	}

	private void pow() {
	}

	private void unm() {
	}

	private void not() {
	}

	private void len() {
	}

	private void concat() {
	}

	private void jmp() {
	}

	private void eq() {
	}

	private void lt() {
	}

	private void le() {
	}

	private void test() {
	}

	private void testset() {
	}

	private void call() {
	}

	private void tailcall() {
	}

	private void ret() {
	}

	private void forloop() {
	}

	private void forprep() {
	}

	private void tforloop() {
	}

	private void setlist() {
	}

	private void close() {
	}

	private void closure() {
	}

	private void vararg() {
	}

}
