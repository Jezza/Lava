package me.jezza.lava.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;

import me.jezza.lava.lang.util.ByteCodeWriter;
import me.jezza.lava.lang.util.ConstantPool;
import me.jezza.lava.runtime.OpCode.Implemented;

/**
 * @author Jezza
 */
@SuppressWarnings("Duplicates")
public final class Interpreter {
	private static final boolean DEBUG_MODE = false;
	private static final Object NIL = "NIL";

	static class CS extends MutableCallSite {
		CS() {
			super(methodType(void.class, Interpreter.class, StackFrame.class, byte.class));
			setTarget(FALLBACK.bindTo(this));
		}

		private void fallback(Interpreter interpreter, StackFrame frame, byte op) throws Throwable {
			MethodHandle dispatch = INSTR_TABLE[op];
			if (dispatch == null)
				throw new IllegalStateException("Opcode not yet implemented: " + op);

			MethodHandle test = MethodHandles.insertArguments(TEST, 3, op);
			MethodHandle target = MethodHandles.dropArguments(dispatch, 2, byte.class);
			MethodHandle guard = MethodHandles.guardWithTest(test, target, getTarget());
			setTarget(guard);

			dispatch.invokeExact(interpreter, frame);
		}

		private static boolean test(Interpreter interpreter, StackFrame frame, byte op, byte expected) {
			return op == expected;
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

			// TEMP
			Field[] fields = OpCode.class.getDeclaredFields();
			List<MethodHandle> handles = new ArrayList<>();
			for (Field field : fields) {
				if (field.isAnnotationPresent(Implemented.class)) {
					try {
						handles.add(lookup.in(Interpreter.class)
								.findVirtual(Interpreter.class, field.getName(), methodType(void.class, StackFrame.class)));
					} catch (NoSuchMethodException | IllegalAccessException e) {
						throw new AssertionError(e);
					}
				} else {
					handles.add(null);
				}
			}
			INSTR_TABLE = handles.toArray(new MethodHandle[0]);
		}
	}

	private static MethodHandle dispatcher() {
		return new CS().dynamicInvoker();
	}

	private static final MethodHandle EXECUTE_MH = dispatcher();

	int status;

	Registers registers;
	Deque<StackFrame> frames;

	Table<Object, Object> globals;

	public Interpreter(StackFrame initialFrame) {
		status = 0;
		registers = new Registers(initialFrame.max, NIL);
		frames = new ArrayDeque<>();
		frames.push(initialFrame);
		globals = new Table<>();
	}

	void move(StackFrame frame, int from, int to) {
		registers.move(frame.base + from, frame.base + to);
	}

	void set(StackFrame frame, int index, Object object) {
		registers.set(frame.base + index, object);
	}

	Object get(StackFrame frame, int index) {
		return registers.get(frame.base + index).value;
	}

	void copy(int source, int dest, int length) {
		if (DEBUG_MODE) {
			System.out.println("COPY (" + source + ") -> (" + dest + "; " + length + ")");
		}
		registers.copy(source, dest, length);
	}

	private StackFrame currentFrame() {
		return frames.peek();
	}

	StackFrame newFrame() {
		StackFrame frame = new StackFrame();
		frames.push(frame);
		return frame;
	}

	StackFrame framePop(int position) {
		StackFrame lastFrame = frames.pop();
		StackFrame frame = currentFrame();
		int results = lastFrame.results;
		if (results > 0) {
//			stackCheck(frame.base + frame.origin + frame.results);
			copy(lastFrame.base + position, frame.base + frame.origin, results);
//			do {
//				move(frame, lastFrame.top - results, frame.top++);
//			} while (--results > 0);
		}
//		// Null out elements that no longer have an associated frame.
//		registers.clear(lastFrame.base, lastFrame.max);
//		for (int i = frame.top; i < lastFrame.top; i++) {
//			stack[i] = NIL;
//		}
		return lastFrame;
	}

	private void dispatchNext(MethodHandle next, StackFrame frame) throws Throwable {
		if (DEBUG_MODE) {
			next.invokeExact(this, frame, OpCode.DEBUG);
		}
		next.invokeExact(this, frame, frame.decode1());
	}

	private static final MethodHandle CONST_MH = dispatcher();
	private static final MethodHandle CONST_NIL_MH = dispatcher();
	private static final MethodHandle CONST_TRUE_MH = dispatcher();
	private static final MethodHandle CONST_FALSE_MH = dispatcher();
	private static final MethodHandle GETGLOBAL_MH = dispatcher();
	private static final MethodHandle SETGLOBAL_MH = dispatcher();
	private static final MethodHandle ADD_MH = dispatcher();
	private static final MethodHandle MUL_MH = dispatcher();
	private static final MethodHandle POP_MH = dispatcher();
	private static final MethodHandle DUP_MH = dispatcher();
	private static final MethodHandle MOV_MH = dispatcher();

	private void CONST(StackFrame frame) throws Throwable {
		int poolIndex = frame.decode2();
		int register = frame.decode2();
		Object constant = frame.constants[poolIndex];
		if (DEBUG_MODE) {
			System.out.println("(CONST) k[" + poolIndex + "]=("+ constant + ") -> slot[" + register + ']');
		}
		set(frame, register, constant);
		dispatchNext(CONST_MH, frame);
	}

	private void CONST_NIL(StackFrame frame) throws Throwable {
		int index = frame.decode2();
		if (DEBUG_MODE) {
			System.out.println("(NIL) -> slot[" + index + ']');
		}
		set(frame, index, NIL);
		dispatchNext(CONST_NIL_MH, frame);
	}

	private void CONST_TRUE(StackFrame frame) throws Throwable {
		int index = frame.decode2();
		if (DEBUG_MODE) {
			System.out.println("(CONST_TRUE) -> slot[" + index + ']');
		}
		set(frame, index, Boolean.TRUE);
		dispatchNext(CONST_TRUE_MH, frame);
	}

	private void CONST_FALSE(StackFrame frame) throws Throwable {
		int index = frame.decode2();
		if (DEBUG_MODE) {
			System.out.println("(CONST_FALSE) -> slot[" + index + ']');
		}
		set(frame, index, Boolean.FALSE);
		dispatchNext(CONST_FALSE_MH, frame);
	}

//	private void LOAD_FUNCTION(StackFrame frame) throws Throwable {
//		int index = frame.decode2();
//		stack[target] = stack[from];
//		dispatchNext(MOV_MH, frame);
//		throw new IllegalStateException("NYI");
//	}

	private void GET_GLOBAL(StackFrame frame) throws Throwable {
		int keySlot = frame.decode2();
		int resultSlot = frame.decode2();
		Object key = get(frame, keySlot);
		Object value = globals.get(key);
		if (DEBUG_MODE) {
			System.out.println("(GET_GLOBAL) -> globals[" + key + "] = " + value);
		}
		set(frame, resultSlot, value);
		dispatchNext(GETGLOBAL_MH, frame);
	}

	private void SET_GLOBAL(StackFrame frame) throws Throwable {
		int keySlot = frame.decode2();
		int valueSlot = frame.decode2();
		Object key = get(frame, keySlot);
		Object value = get(frame, valueSlot);
		if (DEBUG_MODE) {
			System.out.println("(SET_GLOBAL) -> globals[" + key + "] = " + value);
		}
		globals.set(key, value);
		dispatchNext(SETGLOBAL_MH, frame);
	}

	private void ADD(StackFrame frame) throws Throwable {
		int target = frame.decode2();
		int leftSlot = frame.decode2();
		int rightSlot = frame.decode2();

		Object leftObj = get(frame, leftSlot);
		Object rightObj = get(frame, rightSlot);

		if (DEBUG_MODE) {
			System.out.println("ADD (s[" + leftSlot + "] = " + leftObj + ") + (s[" + rightSlot + "] = " + rightObj + ')');
		}

		int left = (int) leftObj;
		int right = (int) rightObj;

		set(frame, target, left + right);

		dispatchNext(ADD_MH, frame);
	}

	private void MUL(StackFrame frame) throws Throwable {
		int target = frame.decode2();
		int leftSlot = frame.decode2();
		int rightSlot = frame.decode2();

		Object leftObj = get(frame, leftSlot);
		Object rightObj = get(frame, rightSlot);

		if (DEBUG_MODE) {
			System.out.println("MUL (s[" + leftSlot + "] = " + leftObj + ") * (s[" + rightSlot + "] = " + rightObj + ')');
		}

		int left = (int) leftObj;
		int right = (int) rightObj;

		set(frame, target, left * right);

		dispatchNext(MUL_MH, frame);
	}

	private void MOVE(StackFrame frame) throws Throwable {
		int from = frame.decode2();
		int to = frame.decode2();
		if (DEBUG_MODE) {
			System.out.println("MOVE " + from + " -> " + to);
		}
		move(frame, from, to);
		dispatchNext(MOV_MH, frame);
	}

	private void CALL(StackFrame frame) throws Throwable {
		// Start of all the shit.
		int base = frame.decode2();
		// How many parameters there are.
		int params = frame.decode2();
		// How many returned results are handled.
		int results = frame.decode2();

		Object o = get(frame, base);
		if (DEBUG_MODE) {
			System.out.println("CALL " + o + '(' + params + ')');
		}
		if (o instanceof Callback) {
//			StackFrame newFrame = newFrame();
//			newFrame.top = frame.top;
//			frame.top -= params;
//			newFrame.base = frame.top;
//			newFrame.results = ((Callback) o).call(this, newFrame);
//
//			DEBUG(null);
//			// @TODO Jezza - 29 May 2017: Support frame reordering
//			framePop();
//			DEBUG(null);
			throw new IllegalStateException("java callback");
		} else if (o instanceof LuaChunk) {
			LuaChunk chunk = (LuaChunk) o;
//			if (params > expected) {
//				stackShrink(frame, params - expected);
//			}
			frame.origin = base;
			StackFrame newFrame = newFrame();
			populateFrame(newFrame, chunk);
			newFrame.results = results;
			newFrame.base = frame.base + frame.max;
//			stackCheck(newFrame.base + newFrame.max);
//			Arrays.fill(stack, newFrame.base, newFrame.base + newFrame.max, NIL);
			copy(frame.base + base + 1, newFrame.base, params);
			
//			if (params < expected) {
//				stackBuff(newFrame, expected);
//			}
//			frame.top -= Math.min(params, expected);
//			newFrame.base = frame.top;
		} else {
			throw new IllegalStateException("Expected call object, but got: " + o);
		}
	}

//	private void SET_TABLE(StackFrame frame) throws Throwable {
//		Object table = stackPop(frame);
//		Object key = stackPop(frame);
//		Object value = stackPop(frame);
//
//		System.out.println("Table: " + table);
//		System.out.println("Key: " + key);
//		System.out.println("Value: " + value);
//		throw new IllegalStateException("Not Yet Implemented!");
//	}

	private void RETURN(StackFrame frame) throws Throwable {
		// @MAYBE Jezza - 27 Feb 2018: Should we unroll the last frame?
		if (frames.size() > 1) {
			// @MAYBE Jezza - 20 Jan 2018: Is it acceptable to ignore any arguments given by the bytecode?
			frame.results = frame.decode1();
			int position = frame.decode1();
			if (DEBUG_MODE) {
				System.out.println("RETURN " + frame.results);
			}
			framePop(position);
		} else {
			status = 1;
		}
	}

	private void GOTO(StackFrame frame) throws Throwable {
		frame.pc = frame.decode4();
	}

	private void DEBUG(StackFrame f) throws Throwable {
		if (!DEBUG_MODE) {
			return;
		}
		System.out.println(registers);
	}

	private static String buildStackView(Object[] stack, StackFrame[] frames) {
		StringBuilder b = new StringBuilder();
		b.append("Stack: [");
		int index = 0;
		for (int i = 0, l = stack.length; i < l; i++) {
			if (index >= frames.length) {
				b.append(stack[i]);
			} else {
				StackFrame frame = frames[index];
				if (frame == null) {
					b.append(stack[i]);
				} else if (frame.base == i) {
					b.append('[');
					b.append(stack[i]);
				} else if (frame.base + frame.max - 1 == i) {
					b.append(stack[i]);
					b.append(']');
					index++;
				} else {
					b.append(stack[i]);
				}
			}
			if (i + 1 < l) {
				b.append(", ");
			}
		}
		return b.append(']').toString();
	}

//	private void DUP(StackFrame frame) throws Throwable {
//		stackPush(frame, stack[frame.top - 1]);
//		dispatchNext(DUP_MH, frame);
//	}

//	private void IFNZ(StackFrame frame) throws Throwable {
//		int target = frame.decode4();
//		int value = (int) stackPop(frame);
//		if (value != 0)
//			frame.pc = target;
//	}

	public void execute() throws Throwable {
		while (status == 0) {
			dispatchNext(EXECUTE_MH, currentFrame());
		}
		System.out.println(globals);
	}

//	private static int print(Interpreter interpreter, StackFrame frame) {
//		int parameters = frame.top - frame.base;
//		for (int i = 0; i < parameters; i++)
//			System.out.println(interpreter.stackPop(frame));
//		interpreter.stackPush(frame, "First");
//		interpreter.stackPush(frame, "Second");
//		interpreter.stackPush(frame, "Third");
//		return 3;
//	}
//
//	private static int nativeAdd(Interpreter interpreter, StackFrame frame) {
//		int parameters = frame.top - frame.base;
//		if (parameters != 2)
//			throw new IllegalStateException("got: " + parameters + ", expected: 2");
//		int first = (int) interpreter.stackPop(frame);
//		int second = (int) interpreter.stackPop(frame);
//		interpreter.stackPush(frame, first + second);
//		return 1;
//	}
//
//	private static int toLower(Interpreter interpreter, StackFrame frame) {
//		int parameters = frame.top - frame.base;
//		if (parameters != 1)
//			throw new IllegalStateException("Requires 1 argument.");
//		String o = (String) interpreter.stackPop(frame);
//		interpreter.stackPush(frame, o.toLowerCase());
//		return 1;
//	}
//
//	private static int toUpper(Interpreter interpreter, StackFrame frame) {
//		int parameters = frame.top - frame.base;
//		if (parameters != 1)
//			throw new IllegalStateException("Requires 1 argument.");
//		String o = (String) interpreter.stackPop(frame);
//		interpreter.stackPush(frame, o.toUpperCase());
//		return 1;
//	}

	private static int test(Interpreter interpreter, StackFrame frame) {
		return 0;
	}

//	private static void prep(Interpreter interpreter) {
//		interpreter.globals.put("print", (Callback) Interpreter::print);
//		interpreter.globals.put("native_add", (Callback) Interpreter::nativeAdd);
//		interpreter.globals.put("upper", (Callback) Interpreter::toUpper);
//		interpreter.globals.put("lower", (Callback) Interpreter::toLower);
//		interpreter.globals.put("test", (Callback) Interpreter::test);
//	}

	public static void test(LuaChunk chunk) throws Throwable {
		StackFrame frame = buildFrame(chunk);
		Interpreter interpreter = new Interpreter(frame);
//		prep(interpreter);
		interpreter.execute();
	}

	public static StackFrame buildFrame(LuaChunk chunk) {
		StackFrame frame = new StackFrame();
		populateFrame(frame, chunk);
		return frame;
	}

	public static void populateFrame(StackFrame frame, LuaChunk chunk) {
		frame.code = chunk.code;
		frame.constants = chunk.constants;
		frame.max = chunk.maxStackSize;
	}

	private static void test(StackFrame frame) throws Throwable {
		Interpreter interpreter = new Interpreter(frame);
//		prep(interpreter);
		interpreter.execute();
//		Object o = interpreter.stackPop(interpreter.currentFrame());
//		System.out.println(o);
	}

	private static LuaChunk forLoop() {
		ConstantPool pool = new ConstantPool();
		ByteCodeWriter w = new ByteCodeWriter();
		w.write2(OpCode.CONST, pool.add(10));
		w.write1(OpCode.GOTO);
		int jump = w.mark();
		w.write4(-1);
		int mark = w.mark();
		w.write2(OpCode.CONST, pool.add(-1));
		w.write1(OpCode.ADD);
		int returnJump = w.mark();
		w.patch4(jump, returnJump);
		w.write1(OpCode.DUP);
		w.write4(OpCode.IFNZ, mark);
		w.write1(OpCode.RETURN);

		LuaChunk chunk = new LuaChunk("forLoopChunk");
		chunk.code = w.code();
		chunk.constants = pool.build();
		return chunk;
	}

	private static LuaChunk returnChunk(int count) {
		ConstantPool pool = new ConstantPool();
		ByteCodeWriter w = new ByteCodeWriter();

		for (int i = 0; i < count; i++)
			w.write2(OpCode.CONST, pool.add(i));
		w.write1(OpCode.RETURN);

		LuaChunk chunk = new LuaChunk("returnChunk");
		chunk.code = w.code();
		chunk.constants = pool.build();
		return chunk;
	}

	private static LuaChunk acceptChunk(int count) {
		ConstantPool pool = new ConstantPool();
		ByteCodeWriter w = new ByteCodeWriter();

//		w.write1(Ops.DEBUG);
		for (int i = 0; i < count; i++)
//			w.write1(Ops.PRINT);
			w.write1(OpCode.POP);
//		w.write1(Ops.DEBUG);
		w.write1(OpCode.RETURN);

		LuaChunk chunk = new LuaChunk("acceptChunk");
		chunk.code = w.code();
		chunk.constants = pool.build();
		return chunk;
	}

	public static void main0(String[] args) throws Throwable {
//		File root = new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\main\\resources");
//		LuaChunk chunk = Language.parse(root, "main.lang");

		ConstantPool pool = new ConstantPool();
//
		ByteCodeWriter w = new ByteCodeWriter(0);
		w.write2(OpCode.CONST, pool.add(1));
		w.write2(OpCode.CONST, pool.add("test"));
		w.write1(OpCode.GET_GLOBAL);
		w.write2(OpCode.CALL, 1, 1);
//		w.write1(OpCode.CONST1, pool.add(25));
//		w.write1(OpCode.CONST1, pool.add(25));
//		w.write1(OpCode.CONST1, pool.add(25));
//		w.write1(OpCode.DEBUG);
//		w.write2(OpCode.MOV, 0, 3);
//		w.write1(OpCode.CONST1, pool.add(1));
//		w.write1(OpCode.CONST1, pool.add("test2"));
//		w.write1(OpCode.GETGLOBAL);
//		w.write2(OpCode.CALL, 2, 0);
//		w.write1(OpCode.DEBUG);
//		w.write1(OpCode.CONST1, pool.add("first"));
//		w.write1(OpCode.CONST1, pool.add("second"));
//		w.write1(OpCode.CONST1, pool.add("third"));
//		w.write1(OpCode.CONST1, pool.add(acceptChunk(4)));
//		w.write1(OpCode.DEBUG);
//		w.write2(OpCode.CALL, 3, 0);
//		w.write1(OpCode.DEBUG);
		w.write1(OpCode.RETURN);

		// E  /  R
		// 0  /  1
		// 1  /  0

		StackFrame frame = new StackFrame();
		frame.code = w.code();
		frame.constants = pool.build();
		System.out.println(Arrays.toString(frame.code));

		testFrame(frame, 1);
	}

	public static void testChunk(LuaChunk chunk, int count) throws Throwable {
		StackFrame frame = buildFrame(chunk);
		testFrame(frame, count);
	}

	public static void testFrame(StackFrame frame, int count) throws Throwable {
		Builder builder = LongStream.builder();
		long start;
		for (int i = 0; i < count; i++) {
			start = System.nanoTime();

			frame.pc = 0;
//			frame.top = 0;
			frame.base = 0;
			test(frame);
//			test(chunk);
			builder.accept(System.nanoTime() - start);
		}
		System.out.println(builder.build().summaryStatistics());
		System.out.println("Done!");
	}

	public static final class StackFrame {
		//	private int func;
		private int base;
//		private int top;

		private int max;

		private int results;
		private int origin;

//		private int tailcalls;

		private int pc;
		private byte[] code;

		private Object[] constants;

		StackFrame() {
		}

		byte decode1() {
			return code[pc++];
		}

		int decode2() {
			return (code[pc++] & 0xFF) << 8 |
					(code[pc++] & 0xFF);
		}

		int decode4() {
			return (code[pc++]) << 24 |
					(code[pc++] & 0xFF) << 16 |
					(code[pc++] & 0xFF) << 8 |
					(code[pc++] & 0xFF);
		}
	}

	interface Callback {
		int call(Interpreter interpreter, StackFrame frame);
	}

	public static final class LuaChunk {
		public final String name;

		public int params;
		public int maxStackSize;

		public byte[] code;
		public Object[] constants;
//		LuaChunk[] chunks;

		public LuaChunk(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "LuaChunk{source=\"" + name + "\"}";
		}
	}
}
