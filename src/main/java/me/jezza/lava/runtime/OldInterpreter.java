package me.jezza.lava.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;

import me.jezza.lava.Bypass;

/**
 * @author Jezza
 */
public final class OldInterpreter {
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
			super(methodType(void.class, OldInterpreter.class, int.class));
			this.debug = debug;
			setTarget(FALLBACK.bindTo(this));
		}

		private void fallback(OldInterpreter interpreter, int ops) throws Throwable {
			System.out.println("dispatch: " + debug + " to " + Ops.values()[ops]);

			MethodHandle dispatch = INSTR_TABLE[ops];

			MethodHandle test = MethodHandles.insertArguments(TEST, 2, ops);
			MethodHandle target = MethodHandles.dropArguments(dispatch, 1, int.class);
			MethodHandle guard = MethodHandles.guardWithTest(test, target, getTarget());
			setTarget(guard);

			INSTR_TABLE[ops].invokeExact(interpreter);
		}

		private static boolean test(OldInterpreter unused, int ops, int expected) {
			return ops == expected;
		}

		static final MethodHandle FALLBACK, TEST;
		static final MethodHandle[] INSTR_TABLE;

		static {
			Lookup lookup = Bypass.LOOKUP;
			try {
				FALLBACK = lookup.findVirtual(CS.class, "fallback", methodType(void.class, OldInterpreter.class, int.class));
				TEST = lookup.findStatic(CS.class, "test", methodType(boolean.class, OldInterpreter.class, int.class, int.class));
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new AssertionError(e);
			}
			Ops[] ops = Ops.values();
			MethodHandle[] table = new MethodHandle[ops.length];
			Arrays.setAll(table, i -> {
				try {
					return lookup.findVirtual(OldInterpreter.class, ops[i].name(), methodType(void.class));
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

	private final static MethodHandle EXECUTE_MH = dispatch("CORE");

	boolean end;
	int result;
	int pc;
	final int[] instrs;
	int top;
	final int[] stack;
	final int[] locals;

	public OldInterpreter(int[] instrs, int maxStack, int maxLocals) {
		this.instrs = instrs;
		this.stack = new int[maxStack];
		this.locals = new int[maxLocals];
	}

	private final static MethodHandle CONST_MH = dispatch("CONST");
	private final static MethodHandle LOAD_MH = dispatch("LOAD");
	private final static MethodHandle STORE_MH = dispatch("STORE");
	private final static MethodHandle ADD_MH = dispatch("ADD");

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
		System.out.println(value);
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
		while (!end) {
			EXECUTE_MH.invokeExact(this, instrs[pc++]);
		}
		return result;
	}

	private static void test(int[] codes) throws Throwable {
		OldInterpreter interpreter = new OldInterpreter(codes, 2, 2);
		int result = interpreter.execute();
		if (result != 10) {
			throw new AssertionError();
		}
	}

	public static void main(String[] args) throws Throwable {
		ByteCodeWriter w = new ByteCodeWriter(0);
		w.write(Ops.CONST, 10);
		w.write(Ops.STORE, 0);
		w.write(Ops.GOTO);
		int jump = w.mark();
		w.write(-1);
		int mark = w.mark();
		w.write(Ops.LOAD, 0);
		w.write(Ops.PRINT);
		w.write(Ops.LOAD, 0);
		w.write(Ops.CONST, -1);
		w.write(Ops.ADD);
		w.write(Ops.STORE, 0);
		w.patch(jump, w.mark());
		w.write(Ops.LOAD, 0);
		w.write(Ops.IFNZ, mark);
		w.write(Ops.CONST, 10);
		w.write(Ops.RET);

		int[] codes = w.code();

		Builder builder = LongStream.builder();
		long start;
		for (int i = 0; i < 1; i++) {
			start = System.nanoTime();
			test(codes);
			builder.accept(System.nanoTime() - start);
		}
		System.out.println(builder.build().summaryStatistics());
		System.out.println("done !");
	}

	public static class ByteCodeWriter {
		private static final int GROWTH_RATE = 64;

		private int[] data;
		private int index;

		public ByteCodeWriter(int size) {
			data = new int[size];
			index = 0;
		}

		public void write(int code) {
			if (index >= data.length) {
				int l = data.length;
				int[] newData = new int[l + GROWTH_RATE];
				System.arraycopy(data, 0, newData, 0, l);
				data = newData;
			}
			data[index++] = code;
		}

		public void write(int first, int second) {
			write(first);
			write(second);
		}

		public void write(int first, int second, int third) {
			write(first);
			write(second);
			write(third);
		}

		public void write(Ops code) {
			write(code.ordinal());
		}

		public void write(Ops code, int second) {
			write(code.ordinal());
			write(second);
		}

		public void write(Ops code, int second, int third) {
			write(code.ordinal());
			write(second);
			write(third);
		}

		public int mark() {
			return index;
		}

		public void patch(int index, int code) {
			data[index] = code;
		}

		public int[] code() {
			int[] code = new int[index];
			System.arraycopy(data, 0, code, 0, index);
			return code;
		}
	}
}