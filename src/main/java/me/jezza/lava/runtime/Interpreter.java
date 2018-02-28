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
import java.util.Iterator;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;

import me.jezza.lava.Strings;
import me.jezza.lava.lang.ParseTree.Name;
import me.jezza.lava.runtime.OpCode.Implemented;
import me.jezza.lava.runtime.Registers.Slot;

/**
 * @author Jezza
 */
@SuppressWarnings("Duplicates")
public final class Interpreter {
	private static final boolean DEBUG_MODE = true;
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

			// @TODO Jezza - 28 Feb 2018: The annotations are obviously just temporary.
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
		if (DEBUG_MODE) {
			System.out.println("MOVE [" + from + "] -> [" + to + "]");
		}
		registers.move(frame.base + from, frame.base + to);
	}

	void set(StackFrame frame, int index, Object object) {
		if (DEBUG_MODE) {
			System.out.println("SET s[" + index + "] = " + object);
		}
		registers.set(frame.base + index, object);
	}

	Slot raw(StackFrame frame, int index) {
		Slot slot = registers.get(frame.base + index);
		if (DEBUG_MODE) {
			System.out.println("GET s[" + index + "] = " + slot);
		}
		return slot;
	}

	Object get(StackFrame frame, int index) {
		return raw(frame, index).value;
	}

	void copy(int source, int dest, int length) {
		if (DEBUG_MODE) {
			System.out.println("COPY " + source + " -> " + dest + " (" + length + ')');
		}
		registers.copy(source, dest, length);
	}

	void move(int source, int dest, int length) {
		if (DEBUG_MODE) {
			System.out.println("MOVE_DESTROY " + source + " -> " + dest + " (" + length + ')');
		}
		registers.move(source, dest, length);
	}

	private StackFrame currentFrame() {
		return frames.peek();
	}

	StackFrame newFrame() {
		StackFrame frame = new StackFrame();
		frames.push(frame);
		return frame;
	}

	StackFrame framePop(int expected, int results, int position) {
		StackFrame lastFrame = frames.pop();
		StackFrame frame = currentFrame();
		// If no results are expected, just toss the frame.
		if (expected == 0) {
			return lastFrame;
		}
		int destination = frame.base + frame.origin;

		move(lastFrame.base + position, destination, Math.min(expected, results));

		registers.clear(destination + results, lastFrame.base + lastFrame.max);
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
	private static final MethodHandle LOAD_FUNC_MH = dispatcher();
	private static final MethodHandle GET_GLOBAL_MH = dispatcher();
	private static final MethodHandle SET_GLOBAL_MH = dispatcher();
	private static final MethodHandle ADD_MH = dispatcher();
	private static final MethodHandle MUL_MH = dispatcher();
	private static final MethodHandle MOVE_MH = dispatcher();
	private static final MethodHandle GET_UPVAL_MH = dispatcher();
	private static final MethodHandle SET_UPVAL_MH = dispatcher();

	private void CONST(StackFrame frame) throws Throwable {
		int poolIndex = frame.decode2();
		int register = frame.decode2();
		Object constant = frame.constants[poolIndex];
		set(frame, register, constant);
		dispatchNext(CONST_MH, frame);
	}

	private void CONST_NIL(StackFrame frame) throws Throwable {
		int index = frame.decode2();
		set(frame, index, NIL);
		dispatchNext(CONST_NIL_MH, frame);
	}

	private void CONST_TRUE(StackFrame frame) throws Throwable {
		int index = frame.decode2();
		set(frame, index, Boolean.TRUE);
		dispatchNext(CONST_TRUE_MH, frame);
	}

	private void CONST_FALSE(StackFrame frame) throws Throwable {
		int index = frame.decode2();
		set(frame, index, Boolean.FALSE);
		dispatchNext(CONST_FALSE_MH, frame);
	}

	private void LOAD_FUNC(StackFrame frame) throws Throwable {
		int poolIndex = frame.decode2();
		int register = frame.decode2();
		Object constant = frame.constants[poolIndex];
		LuaFunction function;
		if (constant instanceof LuaChunk) {
			Name[] names = ((LuaChunk) constant).upvalues;
			Slot[] values = new Slot[names.length];
			for (int i = 0, l = names.length; i < l; i++) {
				Name name = names[i];
				// Minus one, because when the semantic analysis runs, it expects it to be
				// in one more stack frame, but we're not in one yet.
				int level = name.level - 1;
				int index = name.index;

				// @TODO Jezza - 28 Feb 2018: Level check (> 0) and assert.

				Iterator<StackFrame> it = frames.iterator();
				StackFrame current = it.next();
				while (level-- > 0) {
					if (!it.hasNext()) {
						throw new IllegalStateException("Illegal upvalue: " + name);
					}
					current = it.next();
				}

				values[i] = raw(current, index);
			}
			function = new LuaFunction(constant, globals, values);
		} else {
			function = new LuaFunction(constant, globals, new Slot[0]);
		}
		set(frame, register, function);
		dispatchNext(CONST_MH, frame);
	}

	private static final class LuaFunction {
		final Object function;

		Table<Object, Object> environment;
		Slot[] values;

		public LuaFunction(Object function, Table<Object, Object> environment, Slot[] values) {
			this.function = function;
			this.environment = environment;
			this.values = values;
		}

		@Override
		public String toString() {
//			, environment={}
			return Strings.format("LuaFunction{function={}, values={}}",
					function,
//					environment,
					Arrays.toString(values));
		}
	}

	private void GET_GLOBAL(StackFrame frame) throws Throwable {
		int keySlot = frame.decode2();
		int resultSlot = frame.decode2();
		Object key = get(frame, keySlot);
		Object value = globals.get(key);
		if (DEBUG_MODE) {
			System.out.println("(GET_GLOBAL) -> globals[" + key + "] = " + value);
		}
		set(frame, resultSlot, value);
		dispatchNext(GET_GLOBAL_MH, frame);
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
		dispatchNext(SET_GLOBAL_MH, frame);
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
		dispatchNext(MOVE_MH, frame);
	}

	private void GET_UPVAL(StackFrame frame) throws Throwable {
		int index = frame.decode2();
		int register = frame.decode2();

		// @CLEANUP Jezza - 28 Feb 2018: Kind of a magic number, but the stack is bounded by the frame.base, and we know the funciton is just one level below that.
		// We just simply grab that to access the stuffs.
		Object object = get(frame, -1);

		if (DEBUG_MODE) {
			System.out.println("GET_UPVAL u[" + index + "] -> " + register + " :: " + object);
		}

		LuaFunction function = (LuaFunction) object;

		set(frame, register, function.values[index].value);
		dispatchNext(GET_UPVAL_MH, frame);
	}

	private void SET_UPVAL(StackFrame frame) throws Throwable {
		int register = frame.decode2();
		int index = frame.decode2();

		// @CLEANUP Jezza - 28 Feb 2018: Kind of a magic number, but the stack is bounded by the frame.base, and we know the funciton is just one level below that.
		// We just simply grab that to access the stuffs.
		Object object = get(frame, -1);
		
		if (DEBUG_MODE) {
			System.out.println("SET_UPVAL u[" + index + "] -> " + register + " :: " + object);
		}

		LuaFunction function = (LuaFunction) object;

		function.values[index].value = get(frame, register);
		dispatchNext(SET_UPVAL_MH, frame);
	}

	private void CALL(StackFrame frame) throws Throwable {
		// Start of all the shit.
		int base = frame.decode2();
		// How many parameters there are.
		int params = frame.decode2();
		// Expected result count
		int expected = frame.decode2();

		Object target = get(frame, base);
		if (DEBUG_MODE) {
			System.out.println("CALL s[" + base + "] = " + target + '(' + params + ')');
		}

		Object o;
		if (target instanceof LuaFunction) {
			o = ((LuaFunction) target).function;
		} else if (target instanceof Callback) {
			o = target;
		} else {
			// @TODO Jezza - 28 Feb 2018: Would be pretty cool if we could give user code a chance to completely take over the stack.
			o = target;
		}

		if (o instanceof Callback) {
			frame.origin = base;
			StackFrame newFrame = newFrame();
//			newFrame.func = base;
			newFrame.max = params;
			newFrame.base = frame.base + frame.max;
			copy(frame.base + base + 1, newFrame.base, params);

			RegisterList list = new RegisterList(registers, newFrame.base, newFrame.base + newFrame.max);

			RegisterList resultRegisters = ((Callback) o).call(this, list, newFrame);
			if (DEBUG_MODE) {
				System.out.println("NATIVE RETURN (" + resultRegisters.size() + ") : " + resultRegisters);
			}
			int results = resultRegisters.size();

			// @TODO Jezza - 29 May 2017: Support frame reordering
			framePop(expected, results, resultRegisters.from - newFrame.base);
		} else if (o instanceof LuaChunk) {
			LuaChunk chunk = (LuaChunk) o;
//			if (params > expected) {
//				stackShrink(frame, params - expected);
//			}
			frame.origin = base;
			StackFrame newFrame = newFrame();
			populateFrame(newFrame, chunk);
//			newFrame.func = base;
			newFrame.expected = expected;
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
			int results = frame.decode1();
			int position = frame.decode1();
			int expected = frame.expected;
			if (DEBUG_MODE) {
				System.out.println("RETURN " + results + " -> s[" + position + ']');
			}
			framePop(expected, results, position);
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
		if (DEBUG_MODE) {
			System.out.println(globals);
		}
	}

	private static RegisterList print(Interpreter interpreter, RegisterList registers, StackFrame frame) {
		for (Object value : registers) {
			System.out.println(value);
		}
		return RegisterList.EMPTY;
	}

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
	private static RegisterList toLower(Interpreter interpreter, RegisterList registers, StackFrame frame) {
		int parameters = registers.size();
		if (parameters != 1) {
			throw new IllegalStateException("Requires 1 argument (" + parameters + ").");
		}
		String value = registers.getString(0);
		String lowerCase = value.toLowerCase();
		registers.set(0, lowerCase);
		return registers.subList(0, 1);
	}

	private static RegisterList toUpper(Interpreter interpreter, RegisterList registers, StackFrame frame) {
		int parameters = registers.size();
		if (parameters != 1) {
			throw new IllegalStateException("Requires 1 argument.");
		}
		String value = registers.getString(0);
		String upperCase = value.toUpperCase();
		registers.set(0, upperCase);
		return registers.subList(0, 1);
	}

	private static RegisterList flood(Interpreter interpreter, RegisterList registers, StackFrame frame) {
		if (registers.size() != 1) {
			throw new IllegalStateException("Requires 1 argument.");
		}
		int value = registers.getInt(0);
		registers.clear();
		for (int i = 0; i < value; i++) {
			registers.set(i, i);
		}
		return registers.subList(0, value);
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
		interpreter.globals.set("print", (Callback) Interpreter::print);
		interpreter.globals.set("lower", (Callback) Interpreter::toLower);
		interpreter.globals.set("flood", (Callback) Interpreter::flood);
		interpreter.execute();
//		Object o = interpreter.stackPop(interpreter.currentFrame());
//		System.out.println(o);
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
		private int base;
//		private int func;

		private int max;

		private int expected;
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
		RegisterList call(Interpreter interpreter, RegisterList registers, StackFrame frame);
	}

	public static final class LuaChunk {
		public final String name;

		public int maxStackSize;

		public byte[] code;
		public Object[] constants;
//		LuaChunk[] chunks;

		public Name[] upvalues;

		public LuaChunk(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "LuaChunk{source=\"" + name + "\"}";
		}
	}

	public static final class RegisterList implements Iterable<Object> {
		public static final RegisterList EMPTY = new RegisterList(null, 0, 0);

		private final Registers registers;
		private final int from;
		private final int to;

		RegisterList(Registers registers, int from, int to) {
			this.registers = registers;
			this.from = from;
			this.to = to;
		}

		public int size() {
			return to - from;
		}

		public void set(int index, Object value) {
			registers.set(from + index, value);
		}

		public RegisterList subList(int from, int to) {
			// @TODO Jezza - 28 Feb 2018: Range check, make sure that they're returning an allowed subset of the current range.
			return new RegisterList(registers, this.from + from, this.from + to);
		}

		public Object get(int index) {
			if (registers == null) {
				throw new IndexOutOfBoundsException();
			}
			// @TODO Jezza - 28 Feb 2018: Bounds check, etc
			return registers.get(from + index).value;
		}

		public String getString(int index) {
			// @TODO Jezza - 28 Feb 2018: Lua String conversion?
			return (String) get(index);
		}

		public String optString(int index, String defaultValue) {
			// @TODO Jezza - 28 Feb 2018: Lua String conversion?
			Object value = get(index);
			if (value == null || !(value instanceof String)) {
				return defaultValue;
			}
			return (String) value;
		}

		public int getInt(int index) {
			// @TODO Jezza - 28 Feb 2018: Lua String conversion?
			return (int) get(index);
		}

		public int optInt(int index, int defaultValue) {
			// @TODO Jezza - 28 Feb 2018: Lua String conversion?
			Object value = get(index);
			if (value == null || !(value instanceof Integer)) {
				return defaultValue;
			}
			return (Integer) value;
		}

		public void clear() {
			registers.clear(from, to);
		}

		public void clear(int from, int to) {
			registers.clear(this.from + from, this.from + to);
		}

		@Override
		public Iterator<Object> iterator() {
			return new AbstractIterator<>() {
				int index = 0;

				@Override
				protected Object computeNext() {
					if (index + from == to) {
						return endOfData();
					}
					return get(index++);
				}
			};
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			Iterator<Object> it = iterator();
			if (it.hasNext()) {
				builder.append(it.next());
				while (it.hasNext()) {
					builder.append(", ");
					builder.append(it.next());
				}
			}
			builder.append("]");
			return builder.toString();
		}
	}
}
