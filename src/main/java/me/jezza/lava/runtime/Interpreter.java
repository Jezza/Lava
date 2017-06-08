package me.jezza.lava.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;

/**
 * @author Jezza
 */
public final class Interpreter {

	private static final boolean DEBUG_MODE = true;

	enum Ops {
		CONST1,
		CONST2,
//		LOADF,

		GETGLOBAL,
		ADD,
		POP,
		DUP,
		PUSH,

		// stop instructions
		CALL,
		PRINT,
		DEBUG,
		GOTO,
		IFNZ,
		RET
	}

	static class CS extends MutableCallSite {
		CS() {
			super(methodType(void.class, Interpreter.class, StackFrame.class, byte.class));
			setTarget(FALLBACK.bindTo(this));
		}

		private void fallback(Interpreter interpreter, StackFrame frame, byte ops) throws Throwable {
			MethodHandle dispatch = INSTR_TABLE[ops];

			MethodHandle test = MethodHandles.insertArguments(TEST, 3, ops);
			MethodHandle target = MethodHandles.dropArguments(dispatch, 2, byte.class);
			MethodHandle guard = MethodHandles.guardWithTest(test, target, getTarget());
			setTarget(guard);

			dispatch.invokeExact(interpreter, frame);
		}

		private static boolean test(Interpreter interpreter, StackFrame frame, byte ops, byte expected) {
			return ops == expected;
		}

		static final MethodHandle FALLBACK, TEST;
		static final MethodHandle[] INSTR_TABLE;

		static {
			Lookup lookup = MethodHandles.lookup();
			try {
				FALLBACK = lookup.findVirtual(CS.class, "fallback", methodType(void.class, Interpreter.class, StackFrame.class, byte.class));
				TEST = lookup.findStatic(CS.class, "test", methodType(boolean.class, Interpreter.class, StackFrame.class, byte.class, byte.class));
//				MethodHandle test = lookup.findStatic(CS.class, "test", methodType(boolean.class, byte.class, byte.class));
//				TEST = MethodHandles.dropArguments(test, 0, Interpreter.class, StackFrame.class);
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new AssertionError(e);
			}
			Ops[] ops = Ops.values();
			MethodHandle[] table = new MethodHandle[ops.length];
			Arrays.setAll(table, i -> {
				try {
					return lookup.in(Interpreter.class).findVirtual(Interpreter.class, ops[i].name(), methodType(void.class, StackFrame.class));
				} catch (NoSuchMethodException | IllegalAccessException e) {
					throw new AssertionError(e);
				}
			});
//			Field[] fields = OpCodes.class.getDeclaredFields();
//			MethodHandle[] table = new MethodHandle[fields.length];
//			for (int i = 0, l = fields.length; i < l; i++) {
//				String name = fields[i].getName().substring(3).toLowerCase();
//				try {
//					table[i] = lookup.findVirtual(VM.class, name, methodType(void.class));
//				} catch (NoSuchMethodException | IllegalAccessException e) {
//					throw new AssertionError(e);
//				}
//			}
//			INSTR_TABLE = table;
			INSTR_TABLE = table;
		}
	}

	private static MethodHandle dispatcher() {
		return new CS().dynamicInvoker();
	}

	private static final MethodHandle EXECUTE_MH = dispatcher();

	private static final int STACK_GROWTH_RATE = 8;
	private static final int FRAME_GROWTH_RATE = 8;

	int status;

	Object[] stack;

	int frameIndex;
	StackFrame[] frames;

	Map<Object, Object> globals;

	public Interpreter() {
		this(new StackFrame());
	}

	public Interpreter(StackFrame initialFrame) {
		status = 0;
		stack = new Object[0];
		frameIndex = 1;
		frames = new StackFrame[]{initialFrame};
		globals = new HashMap<>();
	}

	void stackPush(StackFrame frame, Object o) {
		stackCheck(frame.top + 1);
		stack[frame.top++] = o;
	}

	void stackBuff(StackFrame frame, int count) {
		stackCheck(frame.top + count);
		frame.top += count;
	}

	void stackShrink(StackFrame frame, int count) {
		// Null out elements that no longer have an associated frame.
		while (--count >= 0)
			stack[frame.top--] = null;
//		frame.top -= count;
	}

	Object stackPop(StackFrame frame) {
		if (frame.base >= frame.top)
			throw new IllegalStateException("No such element");
		Object result = stack[--frame.top];
		stack[frame.top] = null;
		return result;
	}

	void stackCheck(int length) {
		if (length >= stack.length) {
			int l = stack.length;
			Object[] newData = new Object[l + STACK_GROWTH_RATE];
			System.arraycopy(stack, 0, newData, 0, l);
			stack = newData;
		}
	}

	private StackFrame currentFrame() {
		return frames[frameIndex - 1];
	}

	StackFrame newFrame() {
		frameCheck(1);
		StackFrame frame = new StackFrame();
		frames[frameIndex++] = frame;
		return frame;
	}

	void frameCheck(int length) {
		if (frameIndex + length >= frames.length) {
			int l = frames.length;
			StackFrame[] newData = new StackFrame[l + FRAME_GROWTH_RATE];
			System.arraycopy(frames, 0, newData, 0, l);
			frames = newData;
		}
	}

	StackFrame framePop() {
		StackFrame lastFrame = frames[--frameIndex];
		frames[frameIndex] = null;
		if (lastFrame.results != 0) {
			StackFrame frame = currentFrame();
			int results = lastFrame.results; // Math.min(lastFrame.results, lastFrame.top - frame.top);
			System.arraycopy(stack, lastFrame.top - results, stack, frame.top, lastFrame.results);
			frame.top += results;
			// Null out elements that no longer have an associated frame.
			for (int i = frame.top; i < lastFrame.top; i++)
				stack[i] = null;
//			stackShrink(frame, lastFrame.top - frame.top);
		}
		return lastFrame;
	}

	private void dispatchNext(MethodHandle next, StackFrame frame) throws Throwable {
		byte op = frame.decode1();
		next.invokeExact(this, frame, op);
	}

	private static final MethodHandle CONST1_MH = dispatcher();
	private static final MethodHandle CONST2_MH = dispatcher();
	private static final MethodHandle GETGLOBAL_MH = dispatcher();
	private static final MethodHandle ADD_MH = dispatcher();
	private static final MethodHandle POP_MH = dispatcher();
	private static final MethodHandle DUP_MH = dispatcher();
	private static final MethodHandle MOV_MH = dispatcher();

	private void CONST1(StackFrame frame) throws Throwable {
		int value = frame.decode1();
		stackPush(frame, frame.constants[value]);
		dispatchNext(CONST1_MH, frame);
	}

	private void CONST2(StackFrame frame) throws Throwable {
		int value = frame.decode2();
		stackPush(frame, frame.constants[value]);
		dispatchNext(CONST2_MH, frame);
	}

	private void GETGLOBAL(StackFrame frame) throws Throwable {
		Object key = stackPop(frame);
		Object value = globals.get(key);
		stackPush(frame, value);
		dispatchNext(GETGLOBAL_MH, frame);
	}

	private void ADD(StackFrame frame) throws Throwable {
		int second = (int) stackPop(frame);
		int first = (int) stackPop(frame);
		stackPush(frame, first + second);
		dispatchNext(ADD_MH, frame);
	}

	private void POP(StackFrame frame) throws Throwable {
		stackPop(frame);
		dispatchNext(POP_MH, frame);
	}

	private void PUSH(StackFrame frame) throws Throwable {
		int from = frame.base + frame.decode2();
		stackPush(frame, stack[from]);
		dispatchNext(MOV_MH, frame);
	}

//	private void LOADF(StackFrame frame) throws Throwable {
//		int index = frame.decode2();
//		stack[target] = stack[from];
//		dispatchNext(MOV_MH, frame);
//	}

	private void PRINT(StackFrame frame) throws Throwable {
		stackPop(frame);
//		System.out.println(stackPop(frame));
	}

	private void CALL(StackFrame frame) throws Throwable {
		int params = frame.decode2();
		int results = frame.decode2();
		Object o = stackPop(frame);
		if (o instanceof Callback) {
			StackFrame newFrame = newFrame();
			newFrame.top = frame.top;
			frame.top -= params;
			newFrame.base = frame.top;
			newFrame.results = ((Callback) o).call(this, newFrame);
			// TODO: 29/05/2017 Support frame reordering
			framePop();
		} else if (o instanceof LuaChunk) {
			LuaChunk chunk = (LuaChunk) o;
			int expectedCount = chunk.paramCount;
			if (params > expectedCount) {
				for (int i = expectedCount; i < params; i++) {
					// TODO: 31/05/2017 - need a faster method for shrinking the stack a given size
					stackPop(frame);
				}
			}
			StackFrame newFrame = newFrame();
			newFrame.instrs = chunk.code;
			newFrame.constants = chunk.constants;
			newFrame.top = frame.top;

			if (params < expectedCount) {
				for (int i = params; i < expectedCount; i++) {
					// TODO: 31/05/2017 - need a faster method for growing the stack a given size
					stackPush(newFrame, null);
				}
			}

			frame.top -= Math.min(params, expectedCount);
			newFrame.base = frame.top;
			newFrame.results = results;
		} else {
			throw new IllegalStateException("Illegal object on stack");
		}
	}

	private void RET(StackFrame frame) throws Throwable {
		if (frameIndex > 1) {
			framePop();
		} else {
			status = 1;
		}
	}

	private void GOTO(StackFrame frame) throws Throwable {
		frame.pc = frame.decode4();
	}

	private void DEBUG(StackFrame f) throws Throwable {
		if (!DEBUG_MODE)
			return;
		Object[] stack = this.stack;
		StackFrame[] frames = this.frames;
		StringBuilder b = new StringBuilder();
		b.append("Stack: [");
		int stackIndex = 0;
		for (int i = 0; i < frameIndex; i++) {
			StackFrame frame = frames[i];
			b.append('[');
			while (frame.base <= stackIndex && stackIndex < frame.top) {
				b.append(String.valueOf(stack[stackIndex++]));
				if (stackIndex < frame.top)
					b.append(", ");
			}
			b.append(']');
			if (i + 1 < frameIndex)
				b.append(", ");
		}
		b.append(']');
		System.out.println(b);
	}

	private void DUP(StackFrame frame) throws Throwable {
		stackPush(frame, stack[frame.top - 1]);
		dispatchNext(DUP_MH, frame);
	}

	private void IFNZ(StackFrame frame) throws Throwable {
		int target = frame.decode4();
		int value = (int) stackPop(frame);
		if (value != 0)
			frame.pc = target;
	}

	public void execute() throws Throwable {
		while (status == 0) {
			StackFrame frame = currentFrame();
			EXECUTE_MH.invokeExact(this, frame, frame.decode1());
		}
	}

	private static int print(Interpreter interpreter, StackFrame frame) {
		int parameters = frame.top - frame.base;
//		for (int i = 0; i < parameters; i++)
//			System.out.println(interpreter.stackPop(frame));
		interpreter.stackPush(frame, "Native!");
		return 1;
	}

	private static int nativeAdd(Interpreter interpreter, StackFrame frame) {
		int parameters = frame.top - frame.base;
		if (parameters != 2)
			throw new IllegalStateException("got: " + parameters + ", expected: 2");
		int first = (int) interpreter.stackPop(frame);
		int second = (int) interpreter.stackPop(frame);
		interpreter.stackPush(frame, first + second);
		return 1;
	}

	private static void prep(Interpreter interpreter) {
		interpreter.globals.put("print", (Callback) Interpreter::print);
		interpreter.globals.put("native_add", (Callback) Interpreter::nativeAdd);
	}

	private static void test(LuaChunk chunk) throws Throwable {
		StackFrame frame = new StackFrame();

		frame.instrs = chunk.code;
		frame.constants = chunk.constants;

		Interpreter interpreter = new Interpreter(frame);
		prep(interpreter);
		interpreter.execute();
	}

	private static void test(StackFrame frame) throws Throwable {
		Interpreter interpreter = new Interpreter(frame);
		prep(interpreter);
		interpreter.execute();
		Object o = interpreter.stackPop(interpreter.currentFrame());
//		System.out.println(o);
	}

	private static LuaChunk forLoop() {
		ConstantPool pool = new ConstantPool();
		ByteCodeWriter w = new ByteCodeWriter(0);
		w.write1(Ops.CONST1, pool.add(10));
		w.write1(Ops.GOTO);
		int jump = w.mark();
		w.write4(-1);
		int mark = w.mark();
		w.write1(Ops.CONST1, pool.add(-1));
		w.write1(Ops.ADD);
		int returnJump = w.mark();
		w.patch4(jump, returnJump);
		w.write1(Ops.DUP);
		w.write4(Ops.IFNZ, mark);
		w.write1(Ops.RET);

		LuaChunk chunk = new LuaChunk("forLoopChunk");
		chunk.paramCount = 0;
		chunk.code = w.code();
		chunk.constants = pool.build();
		return chunk;
	}

	private static LuaChunk returnChunk(int count) {
		ConstantPool pool = new ConstantPool();
		ByteCodeWriter w = new ByteCodeWriter(0);

		for (int i = 0; i < count; i++)
			w.write1(Ops.CONST1, pool.add(i));
		w.write1(Ops.DEBUG);
		w.write1(Ops.RET);

		LuaChunk chunk = new LuaChunk("returnChunk");
		chunk.paramCount = 0;
		chunk.code = w.code();
		chunk.constants = pool.build();
		return chunk;
	}

	private static LuaChunk acceptChunk(int count) {
		ConstantPool pool = new ConstantPool();
		ByteCodeWriter w = new ByteCodeWriter(0);

//		w.write1(Ops.DEBUG);
		for (int i = 0; i < count; i++)
//			w.write1(Ops.PRINT);
			w.write1(Ops.POP);
//		w.write1(Ops.DEBUG);
		w.write1(Ops.RET);

		LuaChunk chunk = new LuaChunk("acceptChunk");
		chunk.paramCount = count;
		chunk.code = w.code();
		chunk.constants = pool.build();
		return chunk;
	}

	public static void main(String[] args) throws Throwable {
		File root = new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\main\\resources");
		LuaChunk chunk = Language.parse(root, "main.lang");

//		ConstantPool pool = new ConstantPool();
//
//		ByteCodeWriter w = new ByteCodeWriter(0);
//		w.write4(Ops.LOADF, 0);
//		w.write1(Ops.CONST1, pool.add(1));
//		w.write1(Ops.CONST1, pool.add(25));
//		w.write1(Ops.CONST1, pool.add(25));
//		w.write1(Ops.CONST1, pool.add(25));
//		w.write1(Ops.DEBUG);
//		w.write2(Ops.MOV, 0, 3);
//		w.write1(Ops.CONST1, pool.add(1));
//		w.write1(Ops.CONST1, pool.add("test2"));
//		w.write1(Ops.GETGLOBAL);
//		w.write2(Ops.CALL, 2, 0);
//		w.write1(Ops.DEBUG);
//		w.write1(Ops.CONST1, pool.add("first"));
//		w.write1(Ops.CONST1, pool.add("second"));
//		w.write1(Ops.CONST1, pool.add("third"));
//		w.write1(Ops.CONST1, pool.add(acceptChunk(4)));
//		w.write1(Ops.DEBUG);
//		w.write2(Ops.CALL, 3, 0);
//		w.write1(Ops.DEBUG);
//		w.write1(Ops.RET);

//		StackFrame frame = new StackFrame();
//		frame.instrs = w.code();
//		frame.constants = pool.build();
//		System.out.println(Arrays.toString(frame.instrs));

		Builder builder = LongStream.builder();
		long start;
		for (int i = 0; i < 1; i++) {
			start = System.nanoTime();

//			frame.pc = 0;
//			frame.top = 0;
//			frame.base = 0;
//			test(frame);
			test(chunk);
			builder.accept(System.nanoTime() - start);
		}
		System.out.println(builder.build().summaryStatistics());
		System.out.println("Done!");
	}

	static class StackFrame {
		//	private int func;
		private int base;
		private int top;

		private int results;
//		private int tailcalls;

		private int pc;
		private byte[] instrs;

		private Object[] constants;

		StackFrame() {
		}

		byte decode1() {
			return instrs[pc++];
		}

		int decode2() {
			return (instrs[pc++] & 0xFF) << 8 |
					(instrs[pc++] & 0xFF);
		}

		int decode4() {
			return (instrs[pc++]) << 24 |
					(instrs[pc++] & 0xFF) << 16 |
					(instrs[pc++] & 0xFF) << 8 |
					(instrs[pc++] & 0xFF);
		}
	}

	interface Callback {
		int call(Interpreter interpreter, StackFrame frame);
	}

	static final class LuaChunk {
		int paramCount;

		byte[] code;
		Object[] constants;
//		LuaChunk[] chunks;


		private final String name;

		public LuaChunk(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "LuaChunk{source=\"" + name + "\"}";
		}
	}
}
