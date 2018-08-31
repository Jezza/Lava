package me.jezza.lava.runtime;

import java.util.Arrays;

import me.jezza.lava.LuaFunction;
import me.jezza.lava.Strings;

/**
 * @author Jezza
 */
final class Registers {
	private static final int GROWTH_RATE = 2;

	private final Object emptyValue;
	private Slot[] registers;

	Registers(int initial, Object emptyValue) {
		assert initial >= 0;
		this.emptyValue = emptyValue;
		registers = new Slot[initial];
		for (int i = 0; i < initial; i++) {
			registers[i] = new Slot(emptyValue);
		}
	}

	void check(int length) {
		int currentLength = registers.length;
		if (length >= currentLength) {
			int newLength = length + GROWTH_RATE;
			registers = Arrays.copyOf(registers, newLength);
			clear(currentLength, newLength);
		}
	}

	void move(int from, int to) {
		if (from != to) {
			registers[to].value = registers[from].value;
		}
	}

	void set(int index, Object object) {
		registers[index].value = object;
	}

	void copy(int source, int dest, int length) {
		shift(source, dest, length, false);
	}

	void move(int source, int dest, int length) {
		shift(source, dest, length, true);
	}

	private void shift(int source, int dest, int length, boolean destroy) {
		Slot[] registers = this.registers;
		for (int i = 0; i < length; i++) {
			registers[dest + i].value = registers[source + i].value;
			if (destroy) {
				registers[source + i] = new Slot(emptyValue);
			}
		}
	}

	Slot raw(int index) {
		return registers[index];
	}

	Object get(int index) {
		return raw(index).value;
	}

	Object[] getRange(int from, int to) {
		// @TODO Jezza - 03 Apr 2018: sanity checks
		int size = to - from;
		Object[] values = new Object[size];
		for (int i = 0; i < size; i++) {
			values[i] = get(from + i);
		}
		return values;
	}

//	public String getString(int index) {
//		// @TODO Jezza - 28 Feb 2018: Lua String conversion?
//		return (String) get(index);
//	}
//
//	public String optString(int index, String defaultValue) {
//		// @TODO Jezza - 28 Feb 2018: Lua String conversion?
//		Object value = get(index);
//		if (value == null || !(value instanceof String)) {
//			return defaultValue;
//		}
//		return (String) value;
//	}
//
//	public int getInt(int index) {
//		// @TODO Jezza - 28 Feb 2018: Lua number conversion?
//		return (int) get(index);
//	}
//
//	public int optInt(int index, int defaultValue) {
//		// @TODO Jezza - 28 Feb 2018: Lua number conversion?
//		Object value = get(index);
//		if (value == null || !(value instanceof Integer)) {
//			return defaultValue;
//		}
//		return (Integer) value;
//	}

	void clear(int start, int to) {
		for (int i = start; i < to; i++) {
			registers[i] = new Slot(emptyValue);
		}
	}

	@Override
	public String toString() {
		return Strings.format("Registers{emptyValue={}, registers={}}",
				emptyValue,
				Arrays.toString(registers));
	}

	static final class Slot {
		Object value;

		public Slot(Object value) {
			this.value = value;
		}

		@Override
		public String toString() {
			int i = System.identityHashCode(this);
			if ("NIL".equals(value)) {
				return "Slot{value=NIL::" + i + '}';
			} else if (value instanceof String) {
				return Strings.format("Slot{value=\"{}\"::" + i +'}',
						value);
			} else if (value instanceof LuaFunction) {
				return Strings.format("Slot{value={}::" + i + '}',
						value);
			} else if (value instanceof Number) {
			return Strings.format("Slot{value={}::" + i + '}',
					value);
		}
//			return Strings.format("Slot{value={}}",
//					value);
			return Strings.format("Slot{value={}::{}::" + i + '}',
					value.getClass().getSimpleName(),
					System.identityHashCode(value));
		}
	}
}
