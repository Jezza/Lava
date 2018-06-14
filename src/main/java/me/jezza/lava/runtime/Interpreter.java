package me.jezza.lava.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;

import me.jezza.lava.Strings;
import me.jezza.lava.lang.ParseTree.Name;
import me.jezza.lava.runtime.OpCode.Implemented;
import me.jezza.lava.runtime.Registers.Slot;
import me.jezza.lava.utils.Numbers;

/**
 * @author Jezza
 */
public final class Interpreter {
	private static final boolean DEBUG_MODE = false;
	private static final Object NIL = "NIL";

	private static final int MAX_1 = Byte.toUnsignedInt((byte) -1);
	private static final int MAX_2 = Short.toUnsignedInt((short) -1);
	private static final int MAX_4 = -1;

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

	void set(StackFrame frame, int index, Object object) {
		if (DEBUG_MODE) {
			System.out.println("SET s[" + index + "] = " + object);
		}
		registers.set(frame.base + index, object);
	}

	Object get(StackFrame frame, int index) {
		return raw(frame, index).value;
	}

	Slot raw(StackFrame frame, int index) {
		Slot slot = registers.raw(frame.base + index);
		if (DEBUG_MODE) {
			System.out.println("GET s[" + index + "] = " + slot);
		}
		return slot;
	}

	Object function(StackFrame frame) {
		int index = frame.func;
		Slot slot = registers.raw(frame.func);
		if (DEBUG_MODE) {
			System.out.println("GET sR[" + index + "] = " + slot);
		}
		return slot.value;
	}

	RegisterView view(StackFrame frame, int from, int to) {
		Object[] values = registers.getRange(frame.base + from, frame.base + to);
		return new RegisterView(values);
	}

	void move(StackFrame frame, int from, int to) {
		if (DEBUG_MODE) {
			System.out.println("MOVE [" + from + "] -> [" + to + ']');
		}
		registers.move(frame.base + from, frame.base + to);
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
		// If no results are expected, just toss the frame.
		if (expected == 0) {
			return lastFrame;
		}
		StackFrame current = currentFrame();

		int destination = current.base + current.origin;

		boolean unbounded = expected == MAX_2;
		current.returnCount = unbounded
				? results
				: -1;

		move(lastFrame.base + position, destination, unbounded
				? results
				: Math.min(expected, results));

		registers.clear(destination + results, lastFrame.base + lastFrame.max);
		return lastFrame;
	}

	private void dispatchNext(MethodHandle next, StackFrame frame) throws Throwable {
		if (DEBUG_MODE) {
			System.out.println(registers);
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
	private static final MethodHandle NEW_TABLE_MH = dispatcher();
	private static final MethodHandle SET_TABLE_MH = dispatcher();
	private static final MethodHandle GET_TABLE_MH = dispatcher();
	private static final MethodHandle EQ_MH = dispatcher();
	private static final MethodHandle LT_MH = dispatcher();
	private static final MethodHandle ADD_MH = dispatcher();
	private static final MethodHandle MUL_MH = dispatcher();
	private static final MethodHandle NOT_MH = dispatcher();
	private static final MethodHandle MOVE_MH = dispatcher();
	private static final MethodHandle GET_UPVAL_MH = dispatcher();
	private static final MethodHandle SET_UPVAL_MH = dispatcher();
	private static final MethodHandle VARARGS_MH = dispatcher();
	private static final MethodHandle AND_MH = dispatcher();
	private static final MethodHandle OR_MH = dispatcher();
	private static final MethodHandle TO_NUMBER_MH = dispatcher();

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
		Object value = frame.constants[poolIndex];
		if (DEBUG_MODE) {
			System.out.println("LOAD_FUNC " + value);
		}
		LuaFunction function;
		if (value instanceof LuaChunk) {
			Name[] names = ((LuaChunk) value).upvalues;
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
			function = new LuaFunction(value, globals, values);
		} else {
			function = new LuaFunction(value, globals, new Slot[0]);
		}
		set(frame, register, function);
		dispatchNext(LOAD_FUNC_MH, frame);
	}

	private void GET_GLOBAL(StackFrame frame) throws Throwable {
		int keySlot = frame.decode2();
		int resultSlot = frame.decode2();
		Object key = get(frame, keySlot);
		Object value = globals.get(key, NIL);
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

	private void NEW_TABLE(StackFrame frame) throws Throwable {
		int register = frame.decode2();
		if (DEBUG_MODE) {
			System.out.println("(NEW_TABLE) -> r[" + register + "] = {}");
		}
		set(frame, register, new Table<>());
		dispatchNext(NEW_TABLE_MH, frame);
	}

	private void GET_TABLE(StackFrame frame) throws Throwable {
		int register = frame.decode2();
		int tableSlot = frame.decode2();
		int keySlot = frame.decode2();
		Object table = get(frame, tableSlot);
		Object key = get(frame, keySlot);
		if (DEBUG_MODE) {
			System.out.println("(GET_TABLE) -> r[" + tableSlot + "] = " + table + " -> r[" + keySlot + "] = " + key);
		}
		if (!(table instanceof Table)) {
			throw new IllegalStateException("NYI");
		}
		Table<Object, Object> target = (Table<Object, Object>) table;
		Object value = target.get(key, NIL);
		set(frame, register, value);
		dispatchNext(GET_TABLE_MH, frame);
	}

	private void SET_TABLE(StackFrame frame) throws Throwable {
		int tableSlot = frame.decode2();
		int keySlot = frame.decode2();
		int valueSlot = frame.decode2();
		Object table = get(frame, tableSlot);
		Object key = get(frame, keySlot);
		Object value = get(frame, valueSlot);
		if (DEBUG_MODE) {
			System.out.println("(SET_TABLE) -> r[" + tableSlot + "] = " + table + " -> r[" + keySlot + "] = " + key + " -> r[" + valueSlot + "] = " + value);
		}
		if (!(table instanceof Table)) {
			throw new IllegalStateException("NYI: " + table + " :: " + table.getClass());
		}
		Table<Object, Object> target = (Table<Object, Object>) table;
		target.set(key, value);
		dispatchNext(SET_TABLE_MH, frame);
	}

	private void ERROR(StackFrame frame) throws Throwable {
		int message = frame.decode2();
		int line = frame.decode2();

		Object o = get(frame, message);

		String value;
		if (o instanceof String) {
			value = (String) o;
		} else {
			throw new IllegalStateException("metamethods NYI");
		}

		StringBuilder builder = new StringBuilder("Lua Error: ");
		builder.append(value);
		if (line != MAX_2) {
			builder.append(" @ Line ").append(line);
		}
		// @TODO Jezza - 04 Apr 2018: Throw a better exception.
		throw new IllegalStateException(builder.toString());
	}

	private void EQ(StackFrame frame) throws Throwable {
		int target = frame.decode2();
		int leftSlot = frame.decode2();
		int rightSlot = frame.decode2();

		Object leftObj = get(frame, leftSlot);
		Object rightObj = get(frame, rightSlot);

		if (DEBUG_MODE) {
			System.out.println("EQ (s[" + leftSlot + "] = " + leftObj + ") + (s[" + rightSlot + "] = " + rightObj + ')');
		}

		set(frame, target, Objects.equals(leftObj, rightObj));

		dispatchNext(EQ_MH, frame);
	}

	private void LT(StackFrame frame) throws Throwable {
		int target = frame.decode2();
		int leftSlot = frame.decode2();
		int rightSlot = frame.decode2();

		Object leftObj = get(frame, leftSlot);
		Object rightObj = get(frame, rightSlot);

		if (DEBUG_MODE) {
			System.out.println("LT (s[" + leftSlot + "] = " + leftObj + ") < (s[" + rightSlot + "] = " + rightObj + ')');
		}

		int left = (int) leftObj;
		int right = (int) rightObj;

		set(frame, target, left < right);

		dispatchNext(LT_MH, frame);
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

	private void NOT(StackFrame frame) throws Throwable {
		int value = frame.decode2();
		int register = frame.decode2();

		Object valueObj = get(frame, value);

		if (DEBUG_MODE) {
			System.out.println("NOT (s[" + value + "] = " + valueObj + ") -> " + register + ')');
		}

		// @TODO Jezza - 10 Mar 2018: Stuffs
		if (!(valueObj instanceof Boolean)) {
			throw new IllegalStateException("Boolean stuffs.");
		}
		Boolean negated = (Boolean) valueObj ? Boolean.FALSE : Boolean.TRUE;
		set(frame, register, negated);
		dispatchNext(NOT_MH, frame);
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

		Object object = function(frame);

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

		Object object = function(frame);

		if (DEBUG_MODE) {
			System.out.println("SET_UPVAL u[" + index + "] -> " + register + " :: " + object);
		}

		LuaFunction function = (LuaFunction) object;

		function.values[index].value = get(frame, register);
		dispatchNext(SET_UPVAL_MH, frame);
	}

	private void VARARGS(StackFrame frame) throws Throwable {
		if (frame.varargs == null) {
			throw new IllegalStateException("Function has no access to varargs in current scope.");
		}

		int index = frame.decode2();
		int register = frame.decode2();

		boolean unpack = index == MAX_2;
		if (unpack) {
			RegisterView varargs = frame.varargs;
			int size = varargs.size();
			registers.check(frame.base + register + size);
			for (int i = 0; i < size; i++) {
				set(frame, register + i, varargs.get(i));
			}
			frame.returnCount = size;
		} else {
			Object object = frame.varargs.get(index);
			if (DEBUG_MODE) {
				System.out.println("VARARGS varargs[" + index + "] = " + object + " -> s[" + register + ']');
			}
			set(frame, register, object);
		}
		dispatchNext(VARARGS_MH, frame);
	}

	private void CALL(StackFrame frame) throws Throwable {
		// Start of all the shit.
		int base = frame.decode2();
		// How many parameters there are.
		int params = frame.decode2();
		// Expected result count
		int expected = frame.decode2();

		boolean expanded = frame.returnCount != -1;
		if (expanded && frame.returnCount > 0) {
			params = params + frame.returnCount - 1;
		}

		Object target = get(frame, base);
		if (DEBUG_MODE) {
			System.out.println("CALL s[" + base + "] = " + target + '(' + params + ") :: " + expanded);
		}

		Object o;
		if (target instanceof LuaFunction) {
			o = ((LuaFunction) target).function;
		} else if (target instanceof Callback) {
			o = target;
		} else if (target == NIL) {
			throw new IllegalStateException("Attempted to call NIL");
		} else {
			// @TODO Jezza - 28 Feb 2018: Would be pretty cool if we could give user code a chance to completely take over the stack.
//			o = target;
			throw new IllegalStateException("Target :: " + target);
		}

		int source = frame.base + base;

		if (o instanceof Callback) {
			frame.origin = base;

			StackFrame newFrame = newFrame();
			newFrame.func = source;
			newFrame.base = source + 1;
			newFrame.expected = expected;

			RegisterView view = view(newFrame, 0, params);

			RegisterView resultRegisters = ((Callback) o).call(this, view, newFrame);
			int results = resultRegisters != null
					? resultRegisters.size()
					: 0;

			if (DEBUG_MODE) {
				System.out.println("NATIVE RETURN (" + results + ") :: " + resultRegisters);
			}

			// @TODO Jezza - 03 Apr 2018: move values back into registers
			if (results > 0) {
				for (int i = 0, l = resultRegisters.size(); i < l; i++) {
					Object result = resultRegisters.get(i);
					set(newFrame, i, result);
				}
			}

			// @TODO Jezza - 29 May 2017: Support frame reordering
			framePop(expected, results, 0);
		} else if (o instanceof LuaChunk) {
			frame.origin = base;

			LuaChunk chunk = (LuaChunk) o;
			StackFrame newFrame = newFrame();
			populateFrame(newFrame, chunk);
			newFrame.func = source;
			newFrame.base = source + 1;
			newFrame.expected = expected;

			registers.check(newFrame.base + chunk.maxStackSize);

			if (chunk.varargs && params > chunk.parameterCount) {
				newFrame.varargs = view(newFrame, chunk.parameterCount, params);
			}
		} else {
			throw new IllegalStateException("Expected call object, but got: " + o);
		}
	}

	private void IF_FALSE(StackFrame frame) throws Throwable {
		int register = frame.decode2();
		int target = frame.decode2();

		Object o = get(frame, register);
		// @TODO Jezza - 09 Mar 2018: Eval truth stuffs
		if (o == Boolean.FALSE || o == NIL) {
			frame.pc = target;
		}
	}

	private void IF_TRUE(StackFrame frame) throws Throwable {
		int register = frame.decode2();
		int target = frame.decode2();

		Object o = get(frame, register);
		// @TODO Jezza - 09 Mar 2018: Eval truth stuffs
		if (o != Boolean.FALSE && o != NIL) {
			frame.pc = target;
		}
	}

	private void AND(StackFrame frame) throws Throwable {
		int target = frame.decode2();
		int leftRegister = frame.decode2();
		int rightRegister = frame.decode2();

		Object left = get(frame, leftRegister);
		Object right = get(frame, rightRegister);

		boolean isTrue = left != Boolean.FALSE && left != NIL && right != Boolean.FALSE && right != NIL;
		set(frame, target, isTrue);
		dispatchNext(AND_MH, frame);
	}

	private void OR(StackFrame frame) throws Throwable {
		int target = frame.decode2();
		int leftRegister = frame.decode2();
		int rightRegister = frame.decode2();

		Object left = get(frame, leftRegister);
		Object right = get(frame, rightRegister);

		boolean isTrue = left != Boolean.FALSE && left != NIL || right != Boolean.FALSE && right != NIL;
		set(frame, target, isTrue);
		dispatchNext(OR_MH, frame);
	}

	private void CLOSE_SCOPE(StackFrame frame) throws Throwable {
		int offset = frame.decode2();
		registers.clear(frame.base + offset, frame.base + frame.max);
	}

	private void TO_NUMBER(StackFrame frame) throws Throwable {
		int register = frame.decode2();
		int target = frame.decode2();

		Object value = get(frame, register);

		// @TODO Jezza - 04 Apr 2018: Extract for use externally. (eg BaseLib)
		// @TODO Jezza - 04 Apr 2018: Allow radix/base to be specified.
		Object number;
		if (value instanceof Number) {
			number = value;
		} else if (value instanceof String) {
			String str = (String) value;
			OptionalInt optInt = Numbers.parseInt(str);
			if (optInt.isPresent()) {
				number = optInt.getAsInt();
			} else {
				OptionalDouble optDouble = Numbers.parseDouble(str);
				if (optDouble.isPresent()) {
					number = optDouble.getAsDouble();
				} else {
					number = NIL;
				}
			}
		} else {
			throw new IllegalStateException("metamethods NYI");
		}

		if (DEBUG_MODE) {
			System.out.println("TO_NUMBER s[" + register + "] = " + value + " -> to_number(" + number + ") -> s[" + target + ']');
		}

		set(frame, target, number);
		dispatchNext(TO_NUMBER_MH, frame);
	}

	private void RETURN(StackFrame frame) throws Throwable {
		// @MAYBE Jezza - 27 Feb 2018: Should we unroll the last frame?
		if (frames.size() > 1) {
			// @MAYBE Jezza - 20 Jan 2018: Is it acceptable to ignore any arguments given by the bytecode?
			int expected = frame.expected;
			int results = frame.decode1();
			int position = frame.decode1();
			if (DEBUG_MODE) {
				System.out.println("RETURN " + results + " -> s[" + position + ']');
			}
			framePop(expected, results, position);
		} else {
			status = 1;
		}
	}

	private void GOTO(StackFrame frame) throws Throwable {
		frame.pc = frame.decode2();
	}

	public void execute() throws Throwable {
		while (status == 0) {
			dispatchNext(EXECUTE_MH, currentFrame());
		}
		if (DEBUG_MODE) {
			System.out.println(globals);
		}
	}

	private static RegisterView print(Interpreter interpreter, RegisterView registers, StackFrame frame) {
		for (Object value : registers) {
			System.err.println(value);
		}
		return RegisterView.EMPTY;
	}

//	private static int nativeAdd(Interpreter interpreter, StackFrame frame) {
//		int parameters = frame.top - frame.base;
//		if (parameters != 2)
//			throw new IllegalStateException("got: " + parameters + ", expected: 2");
//		int first = (int) interpreter.stackPop(frame);
//		int second = (int) interpreter.stackPop(frame);
//		interpreter.stackPush(frame, first + second);
//		return 1;
//	}

	private static RegisterView toLower(Interpreter interpreter, RegisterView registers, StackFrame frame) {
		int parameters = registers.size();
		if (parameters != 1) {
			throw new IllegalStateException("Requires 1 argument (" + parameters + ").");
		}
		String value = registers.getString(0);
		String lowerCase = value.toLowerCase();
		registers.set(0, lowerCase);
		return registers.of(0, 1);
	}

	private static RegisterView toUpper(Interpreter interpreter, RegisterView registers, StackFrame frame) {
		int parameters = registers.size();
		if (parameters != 1) {
			throw new IllegalStateException("Requires 1 argument.");
		}
		String value = registers.getString(0);
		String upperCase = value.toUpperCase();
		registers.set(0, upperCase);
		return registers.of(0, 1);
	}

	private static RegisterView flood(Interpreter interpreter, RegisterView registers, StackFrame frame) {
		if (registers.size() != 1) {
			throw new IllegalStateException("Requires 1 argument.");
		}
		int value = registers.getInt(0);
		for (int i = 0; i < value; i++) {
			registers.set(i, i);
		}
		return registers.of(0, value);
	}

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

	private static void test(StackFrame frame) throws Throwable {
		Interpreter interpreter = new Interpreter(frame);
		interpreter.globals.set("print", (Callback) Interpreter::print);
		interpreter.globals.set("lower", (Callback) Interpreter::toLower);
		interpreter.globals.set("flood", (Callback) Interpreter::flood);
		interpreter.execute();
	}

	public static final class StackFrame {
		private int base;
		private int func;
		private int origin;

		private int max;

		private int expected;

		private int returnCount;

//		private int tailcalls;

		private RegisterView varargs;

		private int pc;
		private byte[] code;

		private Object[] constants;

		StackFrame() {
			returnCount = -1;
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
		RegisterView call(Interpreter interpreter, RegisterView registers, StackFrame frame);
	}

	public static final class LuaChunk {
		public final String name;

		public int maxStackSize;

		public int parameterCount;
		public boolean varargs;

		public byte[] code;
		public Object[] constants;

		public Name[] upvalues;

		public LuaChunk(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "LuaChunk{source=\"" + name + "\"}";
		}
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
			StringBuilder b = new StringBuilder();
			b.append('[');
			for (int i = 0, l = values.length; i < l; i++) {
				if (values[i].value == this) {
					b.append("{{SELF}}");
				} else {
					b.append(values[i]);
				}
				if (i + 1 != l) {
					b.append(',');
				}
			}
			b.append(']');

			return Strings.format("LuaFunction{function={}, values={}}",
					function,
//					environment,
					b);
		}
	}

//	public static final class RegisterList implements Iterable<Object> {
//		public static final RegisterList EMPTY = new RegisterList(null, 0, 0);
//
//		public RegisterList(Registers registers, int from, int to) {
//			Slot[] range = registers.range(from, to - from);
//		}
//
//		public RegisterList(Slot[] slots) {
//			
//		}
//
//		@Override
//		public Iterator<Object> iterator() {
//			return null;
//		}
//	}

	public static final class RegisterView implements Iterable<Object> {
		public static final RegisterView EMPTY = null;

		// @TODO Jezza - 03 Apr 2018: 
		// @CLEANUP Jezza - 03 Apr 2018: 
		private final List<Object> values;

		RegisterView(Object[] values) {
			this.values = new ArrayList<>();
			Collections.addAll(this.values, values);
		}

		public int size() {
			return values.size();
		}

		public Object get(int index) {
			return values.get(index);
		}

		public Object set(int index, Object value) {
			return values.set(index, value);
		}

		public RegisterView of(int from, int to) {
			// @TODO Jezza - 28 Feb 2018: Range check, make sure that they're returning an allowed subset of the current range.
			return new RegisterView(values.subList(from, to).toArray(new Object[0]));
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
			// @TODO Jezza - 28 Feb 2018: Lua number conversion?
			return (int) get(index);
		}

		public int optInt(int index, int defaultValue) {
			// @TODO Jezza - 28 Feb 2018: Lua number conversion?
			Object value = get(index);
			if (value == null || !(value instanceof Integer)) {
				return defaultValue;
			}
			return (Integer) value;
		}

		@Override
		public Iterator<Object> iterator() {
			int size = size();
			return new AbstractIterator<>() {
				int index = 0;

				@Override
				protected Object computeNext() {
					if (index >= size) {
						return endOfData();
					}
					return get(index++);
				}
			};
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append('[');
			Iterator<Object> it = iterator();
			if (it.hasNext()) {
				builder.append(it.next());
				while (it.hasNext()) {
					builder.append(", ");
					builder.append(it.next());
				}
			}
			builder.append(']');
			return builder.toString();
		}
	}
}
