package me.jezza.lava;

import static java.lang.invoke.MethodType.methodType;

import java.io.StringReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.jezza.lava.lang.emitter.ByteCodeWriter;
import me.jezza.lava.lang.emitter.ConstantPool;
import me.jezza.lava.runtime.OpCode;

/**
 * @author Jezza
 */
@SuppressWarnings("Duplicates")
public final class LInterpreter {
	private static final boolean DEBUG_MODE = true;
	private static final Object NIL = "NIL";

	static final int OP_MOVE = 0;
	static final int OP_LOADK = 1;
	static final int _OP_LOADBOOL = 2;
	static final int _OP_LOADNIL = 3;
	static final int _OP_GETUPVAL = 4;
	static final int _OP_GETGLOBAL = 5;
	static final int _OP_GETTABLE = 6;
	static final int _OP_SETGLOBAL = 7;
	static final int _OP_SETUPVAL = 8;
	static final int _OP_SETTABLE = 9;
	static final int _OP_NEWTABLE = 10;
	static final int _OP_SELF = 11;
	static final int _OP_ADD = 12;
	static final int _OP_SUB = 13;
	static final int _OP_MUL = 14;
	static final int _OP_DIV = 15;
	static final int _OP_MOD = 16;
	static final int _OP_POW = 17;
	static final int _OP_UNM = 18;
	static final int _OP_NOT = 19;
	static final int _OP_LEN = 20;
	static final int _OP_CONCAT = 21;
	static final int _OP_JMP = 22;
	static final int _OP_EQ = 23;
	static final int _OP_LT = 24;
	static final int _OP_LE = 25;
	static final int _OP_TEST = 26;
	static final int _OP_TESTSET = 27;
	static final int _OP_CALL = 28;
	static final int _OP_TAILCALL = 29;
	static final int OP_RETURN = 30;
	static final int _OP_FORLOOP = 31;
	static final int _OP_FORPREP = 32;
	static final int _OP_TFORLOOP = 33;
	static final int _OP_SETLIST = 34;
	static final int _OP_CLOSE = 35;
	static final int _OP_CLOSURE = 36;
	static final int _OP_VARARG = 37;
	static final int OP_DEBUG = 38;

	static class CS extends MutableCallSite {
		CS() {
			super(methodType(void.class, LInterpreter.class, StackFrame.class, int.class));
			setTarget(FALLBACK.bindTo(this));
		}

		private void fallback(LInterpreter interpreter, StackFrame frame, int instruction) throws Throwable {
			int op = opcode(instruction);
			MethodHandle dispatch = INSTR_TABLE[op];
			if (dispatch == null) {
				throw new IllegalStateException("Opcode not yet implemented: " + OP_NAMES[op]);
			}

			MethodHandle test = MethodHandles.insertArguments(TEST, 3, op);
//			MethodHandle target = MethodHandles.dropArguments(dispatch, 2, int.class);
			MethodHandle guard = MethodHandles.guardWithTest(test, dispatch, getTarget());
			setTarget(guard);

			dispatch.invokeExact(interpreter, frame, instruction);
		}

		private static boolean test(LInterpreter interpreter, StackFrame frame, int op, int expected) {
			return opcode(op) == expected;
		}

		static final MethodHandle FALLBACK, TEST;
		static final MethodHandle[] INSTR_TABLE;

		static final String[] OP_NAMES;

		static {
			Lookup lookup = MethodHandles.lookup();
			try {
				FALLBACK = lookup.findVirtual(CS.class, "fallback", methodType(void.class, LInterpreter.class, StackFrame.class, int.class));
				TEST = lookup.findStatic(CS.class, "test", methodType(boolean.class, LInterpreter.class, StackFrame.class, int.class, int.class));
//				MethodHandle test = lookup.findStatic(CS.class, "test", methodType(boolean.class, byte.class, byte.class));
//				TEST = MethodHandles.dropArguments(test, 0, Interpreter.class, StackFrame.class);
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new AssertionError(e);
			}

			// TEMP
			List<MethodHandle> handles = new ArrayList<>();
			List<String> names = new ArrayList<>();
			try {
				for (Field field : LInterpreter.class.getDeclaredFields()) {
					String name = field.getName();
					if (name.startsWith("OP_")) {
						int value = (int) field.get(null);
						names.add(value, name);
						MethodHandle handle = lookup.in(LInterpreter.class)
								.findVirtual(LInterpreter.class, name.substring(3), methodType(void.class, StackFrame.class, int.class));
						handles.add(value, handle);
					} else if (name.startsWith("_")) {
						names.add(name.substring(1));
						int value = (int) field.get(null);
						handles.add(value, null);
					}
				}
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
			INSTR_TABLE = handles.toArray(new MethodHandle[0]);
			OP_NAMES = names.toArray(new String[0]);
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

	public LInterpreter() {
		this(new StackFrame());
	}

	public LInterpreter(StackFrame initialFrame) {
		status = 0;
		stack = new Object[0];
		frameIndex = 1;
		frames = new StackFrame[]{initialFrame};
		globals = new HashMap<>();
	}

	void stackMove(StackFrame frame, int from, int to) {

	}


	void stackPush(StackFrame frame, Object o) {
		stackCheck(frame.top + 1);
		stack[frame.top++] = o != null ? o : NIL;
	}

	void stackBuff(StackFrame frame, int count) {
		stackCheck(frame.top + count);
		Object[] stack = this.stack;
		while (--count >= 0)
			stack[frame.top++] = NIL;
	}

	void stackShrink(StackFrame frame, int count) {
		Object[] stack = this.stack;
		while (--count >= 0)
			stack[frame.top--] = NIL;
	}

	Object stackPop(StackFrame frame) {
		if (frame.base >= frame.top)
			throw new IllegalStateException("No such element");
		Object result = stack[--frame.top];
		stack[frame.top] = NIL;
		return result;
	}

	void stackSet(StackFrame frame, int from, int to) {
		if (from != to) {
			stack[frame.base + to] = stack[frame.base + from];
		}
	}

	void stackCheck(int length) {
		if (length >= stack.length) {
			int l = stack.length;
			Object[] newData = new Object[l + STACK_GROWTH_RATE];
			System.arraycopy(stack, 0, newData, 0, l);
//			Arrays.fill(newData, l, newData.length, NIL);
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
		int expected = lastFrame.expected; // Math.min(lastFrame.results, lastFrame.top - frame.top);
		if (expected != 0) {
			StackFrame frame = currentFrame();
			int results = lastFrame.results;
			if (results == 0) {
				stackBuff(frame, expected);
			} else {
//				int diff = expected - (lastFrame.top - frame.top);
//				if (diff > 0) {
//					stackBuff(frame, diff);
//				} else {
//					throw new IllegalStateException("");
				System.arraycopy(stack, (lastFrame.top - results) + expected - 1, stack, frame.top, lastFrame.results);
				frame.top += expected;
//				}
//				stackBuff(frame, expected - lastFrame.top - frame.top);
			}
			// Null out elements that no longer have an associated frame.
			for (int i = frame.top; i < lastFrame.top; i++)
				stack[i] = NIL;
//			stackShrink(frame, lastFrame.top - frame.top);
		}
		return lastFrame;
	}

	private void dispatchNext(MethodHandle next, StackFrame frame) throws Throwable {
		next.invokeExact(this, frame, OP_DEBUG);
		int op = frame.fetchOp();
		System.out.println("OP: " + opcode(op));
		next.invokeExact(this, frame, op);
	}

	private static final MethodHandle CONST1_MH = dispatcher();
	private static final MethodHandle CONST2_MH = dispatcher();
	private static final MethodHandle CONST_NIL_MH = dispatcher();
	private static final MethodHandle CONST_TRUE_MH = dispatcher();
	private static final MethodHandle CONST_FALSE_MH = dispatcher();
	private static final MethodHandle GETGLOBAL_MH = dispatcher();
	private static final MethodHandle ADD_MH = dispatcher();
	private static final MethodHandle MUL_MH = dispatcher();
	private static final MethodHandle POP_MH = dispatcher();
	private static final MethodHandle DUP_MH = dispatcher();
	private static final MethodHandle MOV_MH = dispatcher();

	private void MOVE(StackFrame frame, int op) throws Throwable {
	}

	private void LOADK(StackFrame frame, int op) throws Throwable {
		int a = ARGA(op);
		int b = ARGB(op);
//		if (a != b) {
//			stack[frame.base + a] = frame.constants[frame.base + b];
//		}
//		stack[frame.base + a] = frame.constants[frame.base + b];
		stackPush(frame, frame.constants[b]);
	}

//	private void CONST1(StackFrame frame) throws Throwable {
//		int value = frame.decode1();
//		stackPush(frame, frame.constants[value]);
//		dispatchNext(CONST1_MH, frame);
//	}
//
//	private void CONST2(StackFrame frame) throws Throwable {
//		int value = frame.decode2();
//		stackPush(frame, frame.constants[value]);
//		dispatchNext(CONST2_MH, frame);
//	}
//
//	private void CONST_NIL(StackFrame frame) throws Throwable {
//		stackPush(frame, NIL);
//		dispatchNext(CONST_NIL_MH, frame);
//	}
//
//	private void CONST_TRUE(StackFrame frame) throws Throwable {
//		stackPush(frame, Boolean.TRUE);
//		dispatchNext(CONST_TRUE_MH, frame);
//	}
//
//	private void CONST_FALSE(StackFrame frame) throws Throwable {
//		stackPush(frame, Boolean.FALSE);
//		dispatchNext(CONST_FALSE_MH, frame);
//	}
//
//	private void LOAD_FUNCTION(StackFrame frame) throws Throwable {
////		int index = frame.decode2();
////		stack[target] = stack[from];
////		dispatchNext(MOV_MH, frame);
//		throw new IllegalStateException("NYI");
//	}
//
//	private void GET_GLOBAL(StackFrame frame) throws Throwable {
//		Object key = stackPop(frame);
//		Object value = globals.get(key);
//		stackPush(frame, value);
//		dispatchNext(GETGLOBAL_MH, frame);
//	}
//
//	private void ADD(StackFrame frame) throws Throwable {
//		int second = (int) stackPop(frame);
//		int first = (int) stackPop(frame);
//		stackPush(frame, first + second);
//		dispatchNext(ADD_MH, frame);
//	}
//
//	private void MUL(StackFrame frame) throws Throwable {
//		int second = (int) stackPop(frame);
//		int first = (int) stackPop(frame);
//		stackPush(frame, first * second);
//		dispatchNext(MUL_MH, frame);
//	}
//
//	private void POP(StackFrame frame) throws Throwable {
//		stackPop(frame);
//		dispatchNext(POP_MH, frame);
//	}
//
//	private void MOV(StackFrame frame) throws Throwable {
//		int from = frame.decode2();
//		int to = frame.decode2();
//		stackSet(frame, from, to);
//		dispatchNext(MOV_MH, frame);
//	}
//
//	private void CALL(StackFrame frame) throws Throwable {
//		int params = frame.decode2();
//		int expected = frame.decode2();
//		Object o = stackPop(frame);
//		if (o instanceof Callback) {
//			StackFrame newFrame = newFrame();
//			newFrame.top = frame.top;
//			frame.top -= params;
//			newFrame.base = frame.top;
//			newFrame.expected = expected;
//			newFrame.results = ((Callback) o).call(this, newFrame);
//
//			DEBUG(null);
//			// TODO: 29/05/2017 Support frame reordering
//			framePop();
//			DEBUG(null);
//		} else if (o instanceof LuaChunk) {
//			LuaChunk chunk = (LuaChunk) o;
//			int expectedCount = chunk.paramCount;
//			if (params > expectedCount) {
//				for (int i = expectedCount; i < params; i++) {
//					// TODO: 31/05/2017 - need a faster method for shrinking the stack a given size
//					stackPop(frame);
//				}
//			}
//			StackFrame newFrame = newFrame();
//			newFrame.instrs = chunk.code;
//			newFrame.constants = chunk.constants;
//			newFrame.top = frame.top;
//
//			if (params < expectedCount) {
//				for (int i = params; i < expectedCount; i++) {
//					// TODO: 31/05/2017 - need a faster method for growing the stack a given size
//					stackPush(newFrame, NIL);
//				}
//			}
//
//			frame.top -= Math.min(params, expectedCount);
//			newFrame.base = frame.top;
//			newFrame.expected = expected;
//		} else {
//			throw new IllegalStateException("Expected call object on stack, but got: " + o);
//		}
//	}
//
	private void RETURN(StackFrame frame, int op) throws Throwable {
//		if (frameIndex > 1) {
//			// @TODO Jezza - 20 Jan 2018: Is it acceptable to ignore any arguments given by the bytecode?
//			frame.results = frame.decode2();
//			framePop();
//		} else {
		status = 1;
//		}
	}
//
//	private void GOTO(StackFrame frame) throws Throwable {
//		frame.pc = frame.decode4();
//	}

	private void DEBUG(StackFrame f, int op) throws Throwable {
		if (!DEBUG_MODE)
			return;
		System.out.println(buildStackView(stack, frames, frameIndex));
	}

	private static String buildStackView(Object[] stack, StackFrame[] frames, int frameIndex) {
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
		return b.append(']').toString();
	}

//	private void DUP(StackFrame frame) throws Throwable {
//		stackPush(frame, stack[frame.top - 1]);
//		dispatchNext(DUP_MH, frame);
//	}
//
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
	}

	private static int print(LInterpreter interpreter, StackFrame frame) {
		int parameters = frame.top - frame.base;
		for (int i = 0; i < parameters; i++)
			System.out.println(interpreter.stackPop(frame));
		interpreter.stackPush(frame, "First");
		interpreter.stackPush(frame, "Second");
		interpreter.stackPush(frame, "Third");
		return 3;
	}

	private static int nativeAdd(LInterpreter interpreter, StackFrame frame) {
		int parameters = frame.top - frame.base;
		if (parameters != 2)
			throw new IllegalStateException("got: " + parameters + ", expected: 2");
		int first = (int) interpreter.stackPop(frame);
		int second = (int) interpreter.stackPop(frame);
		interpreter.stackPush(frame, first + second);
		return 1;
	}

	private static int toLower(LInterpreter interpreter, StackFrame frame) {
		int parameters = frame.top - frame.base;
		if (parameters != 1)
			throw new IllegalStateException("Requires 1 argument.");
		String o = (String) interpreter.stackPop(frame);
		interpreter.stackPush(frame, o.toLowerCase());
		return 1;
	}

	private static int toUpper(LInterpreter interpreter, StackFrame frame) {
		int parameters = frame.top - frame.base;
		if (parameters != 1)
			throw new IllegalStateException("Requires 1 argument.");
		String o = (String) interpreter.stackPop(frame);
		interpreter.stackPush(frame, o.toUpperCase());
		return 1;
	}

	private static int test(LInterpreter interpreter, StackFrame frame) {
		return 0;
	}

	private static void prep(LInterpreter interpreter) {
		interpreter.globals.put("print", (Callback) LInterpreter::print);
		interpreter.globals.put("native_add", (Callback) LInterpreter::nativeAdd);
		interpreter.globals.put("upper", (Callback) LInterpreter::toUpper);
		interpreter.globals.put("lower", (Callback) LInterpreter::toLower);
		interpreter.globals.put("test", (Callback) LInterpreter::test);
	}

//	public static void test(LuaChunk chunk) throws Throwable {
//		StackFrame frame = new StackFrame();
//
//		frame.code = chunk.code;
//		frame.constants = chunk.constants;
//
//		LInterpreter interpreter = new LInterpreter(frame);
//		prep(interpreter);
//		interpreter.execute();
//	}

	private static void test(StackFrame frame) throws Throwable {
		LInterpreter interpreter = new LInterpreter(frame);
		prep(interpreter);
		interpreter.execute();
		Object o = interpreter.stackPop(interpreter.currentFrame());
//		System.out.println(o);
	}

	private static LuaChunk forLoop() {
		ConstantPool pool = new ConstantPool();
		ByteCodeWriter w = new ByteCodeWriter();
		w.write1(OpCode.CONST1, pool.add(10));
		w.write1(OpCode.GOTO);
		int jump = w.mark();
		w.write4(-1);
		int mark = w.mark();
		w.write1(OpCode.CONST1, pool.add(-1));
		w.write1(OpCode.ADD);
		int returnJump = w.mark();
		w.patch4(jump, returnJump);
		w.write1(OpCode.DUP);
		w.write4(OpCode.IFNZ, mark);
		w.write1(OpCode.RET);

		LuaChunk chunk = new LuaChunk("forLoopChunk");
		chunk.paramCount = 0;
		chunk.code = w.code();
		chunk.constants = pool.build();
		return chunk;
	}

	private static LuaChunk returnChunk(int count) {
		ConstantPool pool = new ConstantPool();
		ByteCodeWriter w = new ByteCodeWriter();

		for (int i = 0; i < count; i++)
			w.write1(OpCode.CONST1, pool.add(i));
		w.write1(OpCode.RET);

		LuaChunk chunk = new LuaChunk("returnChunk");
		chunk.paramCount = 0;
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
		w.write1(OpCode.RET);

		LuaChunk chunk = new LuaChunk("acceptChunk");
		chunk.paramCount = count;
		chunk.code = w.code();
		chunk.constants = pool.build();
		return chunk;
	}

	public static void main(String[] args) throws Throwable {
		Lua L = new Lua();
		StringReader reader = new StringReader("" +
				"local a = \"Hello\";\n" +
				"local b = \", World\";\n" +
				"local c = a .. b;\n" +
				"print(c);\n");
		Proto proto = Syntax.parser(L, reader, "[[chunk]]");

		StackFrame frame = new StackFrame();

		Slot[] protoK = proto.constants();
		Object[] constants = new Object[protoK.length];
		for (int i = 0, l = protoK.length; i < l; i++) {
			constants[i] = protoK[i].asObject();
		}

		frame.code = proto.code;
		frame.constants = constants;

		LInterpreter interpreter = new LInterpreter(frame);
		interpreter.stackBuff(frame, proto.maxstacksize);
		frame.top -= proto.maxstacksize;
		interpreter.execute();
	}

	static int opcode(int instruction) {
		// POS_OP == 0 (shift amount)
		// SIZE_OP == 6 (opcode width)
		return instruction & 0x3f;
	}

	static int ARGA(int instruction) {
		// POS_A == POS_OP + SIZE_OP == 6 (shift amount)
		// SIZE_A == 8 (operand width)
		return (instruction >>> 6) & 0xff;
	}

	static int ARGB(int instruction) {
		// POS_B == POS_OP + SIZE_OP + SIZE_A + SIZE_C == 23 (shift amount)
		// SIZE_B == 9 (operand width)
		return instruction >>> 23;
	}

	static int ARGC(int instruction) {
		// POS_C == POS_OP + SIZE_OP + SIZE_A == 14 (shift amount)
		// SIZE_C == 9 (operand width)
		return (instruction >>> 14) & 0x1ff;
	}

	static int ARGBx(int instruction) {
		// POS_Bx = POS_C == 14
		// SIZE_Bx == SIZE_C + SIZE_B == 18
		/* No mask required as field occupies the most significant bits of a
		 * 32 bit int. */
		return (instruction >>> 14);
	}

	private static final int SIZE_C = 9;
	private static final int SIZE_B = 9;
	private static final int SIZE_Bx = SIZE_C + SIZE_B;
	private static final int SIZE_A = 8;

	private static final int SIZE_OP = 6;

	private static final int POS_OP = 0;
	private static final int POS_A = POS_OP + SIZE_OP;
	private static final int POS_C = POS_A + SIZE_A;
	private static final int POS_B = POS_C + SIZE_C;
	private static final int POS_Bx = POS_C;

	private static final int MAXARG_Bx = (1 << SIZE_Bx) - 1;
	private static final int MAXARG_sBx = MAXARG_Bx >> 1;    // `sBx' is signed

	static int ARGsBx(int instruction) {
		// As ARGBx but with (2**17-1) subtracted.
		return (instruction >>> 14) - MAXARG_sBx;
	}

	static boolean ISK(int field) {
		// The "is constant" bit position depends on the size of the B and C
		// fields (required to be the same width).
		// SIZE_B == 9
		return field >= 0x100;
	}

	static final class StackFrame {
		//	private int func;
		private int base;
		private int top;

		private int expected;
		private int results;

//		private int tailcalls;

		private int pc;
		private int[] code;

		private Object[] constants;

		StackFrame() {
		}

//		byte decode1() {
//			return instrs[pc++];
//		}

//		int decode2() {
//			return (instrs[pc++] & 0xFF) << 8 |
//					(instrs[pc++] & 0xFF);
//		}

		int fetchOp() {
			return code[pc++];
//			return (instrs[pc++]) << 24 |
//					(instrs[pc++] & 0xFF) << 16 |
//					(instrs[pc++] & 0xFF) << 8 |
//					(instrs[pc++] & 0xFF);
		}
	}

	interface Callback {
		int call(LInterpreter interpreter, StackFrame frame);
	}

	public static final class LuaChunk {
		public final String name;

		public int paramCount;

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
