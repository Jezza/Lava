package me.jezza.lava;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;

/**
 * @author Jezza
 */
public final class Interpreter {
	enum Ops {
		CONST,
		LOAD,
		STORE,
		ADD,

		// stop instructions
		PRINT,
		GOTO,
		IFNZ,
		RET
	}

	static class CS extends MutableCallSite {
		private final String debug;

		public CS(String debug) {
			super(methodType(void.class, Interpreter.class, int.class));
			this.debug = debug;
			setTarget(FALLBACK.bindTo(this));
		}

		private void fallback(Interpreter interpreter, int ops) throws Throwable {
			System.out.println("dispatch: " + this + " " + debug + " to " + ops);

			MethodHandle dispatch = INSTR_TABLE[ops];

			MethodHandle test = MethodHandles.insertArguments(TEST, 2, ops);
			MethodHandle target = MethodHandles.dropArguments(dispatch, 1, int.class);
			MethodHandle guard = MethodHandles.guardWithTest(test, target, getTarget());
			setTarget(guard);

			INSTR_TABLE[ops].invokeExact(interpreter);
		}

		private static boolean test(Interpreter unused, int ops, int expected) {
			return ops == expected;
		}

		static final MethodHandle FALLBACK, TEST;
		static final MethodHandle[] INSTR_TABLE;

		static {
			Lookup lookup = MethodHandles.lookup();
			try {
				FALLBACK = lookup.findVirtual(CS.class, "fallback", methodType(void.class, Interpreter.class, int.class));
				TEST = lookup.findStatic(CS.class, "test", methodType(boolean.class, Interpreter.class, int.class, int.class));
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new AssertionError(e);
			}
			Ops[] ops = Ops.values();
			MethodHandle[] table = new MethodHandle[ops.length];
			Arrays.setAll(table, i -> {
				try {
					return lookup.findVirtual(Interpreter.class, ops[i].name(), methodType(void.class));
				} catch (NoSuchMethodException | IllegalAccessException e) {
					throw new AssertionError(e);
				}
			});
			INSTR_TABLE = table;
		}
	}

	private static MethodHandle dispatch(String debug) {
		return new CS(debug).dynamicInvoker();
	}

	private final static MethodHandle EXECUTE_MH = dispatch("execute");

	boolean end;
	int result;
	int pc;
	final int[] instrs;
	int top;
	final int[] stack;
	final int[] locals;

	public Interpreter(int[] instrs, int maxStack, int maxLocals) {
		this.instrs = instrs;
		this.stack = new int[maxStack];
		this.locals = new int[maxLocals];
	}

	private final static MethodHandle CONST_MH = dispatch("const");
	private final static MethodHandle LOAD_MH = dispatch("load");
	private final static MethodHandle STORE_MH = dispatch("store");
	private final static MethodHandle ADD_MH = dispatch("add");

	private void CONST() throws Throwable {
		int value = instrs[pc++];
		stack[top++] = value;
		CONST_MH.invokeExact(this, instrs[pc++]);
	}
	private void LOAD() throws Throwable {
		int local = instrs[pc++];
		stack[top++] = locals[local];
		LOAD_MH.invokeExact(this, instrs[pc++]);
	}
	private void STORE() throws Throwable {
		int local = instrs[pc++];
		locals[local] = stack[--top];
		STORE_MH.invokeExact(this, instrs[pc++]);
	}
	private void ADD() throws Throwable {
		int v2 = stack[--top];
		int v1 = stack[--top];
		stack[top++] = v1 + v2;
		ADD_MH.invokeExact(this, instrs[pc++]);
	}
	private void PRINT() throws Throwable {
		int value = stack[--top];
		//System.out.println(value);
	}
	private void RET() throws Throwable {
		result = stack[--top];
		//System.out.println("stop !");
		end = true;
	}
	private void GOTO() throws Throwable {
		pc = instrs[pc];
		//System.out.println("stop !");
	}
	private void IFNZ() throws Throwable {
		int target = instrs[pc++];
		int value = stack[--top];
		if (value != 0) {
			pc = target;
		}
		//System.out.println("stop !");
	}




	public int execute(int... args) throws Throwable {
		System.arraycopy(args, 0, locals, 0, args.length);
		int[] instrs = this.instrs;
		while(!end) {
			EXECUTE_MH.invokeExact(this, instrs[pc++]);
		}
		return result;
	}

	private static void test(int[] codes) throws Throwable {
		Interpreter interpreter = new Interpreter(codes, 2, 1);
		int result = interpreter.execute();
		if (result != 10) {
			throw new AssertionError();
		}
	}

	private static int op(Ops opcode) {
		return opcode.ordinal();
	}

	public static void main(String[] args) throws Throwable {
		int[] codes = {
				op(Ops.CONST), 10,  // 0
				op(Ops.STORE), 0,   // 2
				op(Ops.GOTO),  16,  // 4
				op(Ops.LOAD), 0,    // 6
				op(Ops.PRINT),      // 8
				op(Ops.LOAD),  0,   // 9
				op(Ops.CONST), -1,  // 11
				op(Ops.ADD),        // 13
				op(Ops.STORE), 0,   // 14
				op(Ops.LOAD),  0,   // 16
				op(Ops.IFNZ),  6,   // 18
				op(Ops.CONST), 10,  // 20
				op(Ops.RET)         // 22
		};

		Builder builder = LongStream.builder();
		long start;
		for(int i = 0; i < 2_000_000; i++) {
			start = System.nanoTime();
			test(codes);
			builder.accept(System.nanoTime() - start);
		}
		System.out.println(builder.build().summaryStatistics());
		System.out.println("done !");
	}
}