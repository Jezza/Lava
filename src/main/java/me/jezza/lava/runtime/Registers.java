package me.jezza.lava.runtime;

import java.util.Arrays;

import me.jezza.lava.Strings;

/**
 * @author Jezza
 */
final class Registers {
	private static final int GROWTH_RATE = 2;

	private final Object emptyValue;
	private Slot[] registers;

	Registers(int initial, Object emptyValue) {
		assert initial >= 1;
		this.emptyValue = emptyValue;
		registers = new Slot[initial];
		for (int i = 0; i < initial; i++) {
			registers[i] = new Slot(emptyValue);
		}
	}

	private void check(int length) {
		int currentLength = registers.length;
		if (length >= currentLength) {
			int newLength = length + GROWTH_RATE;
			registers = Arrays.copyOf(registers, newLength);
			clear(currentLength, newLength);
		}
	}

	void move(int from, int to) {
		check(to);
		check(from);
		if (from != to) {
			registers[to].value = registers[from].value;
		}
	}

	void set(int index, Object object) {
		check(index);
		registers[index].value = object;
	}

	Slot get(int index) {
		return registers[index];
	}

	void copy(int source, int dest, int length) {
		check(source + length);
		check(dest + length);
		System.arraycopy(registers, source, registers, dest, length);
	}
	
	void clear(int start, int length) {
		for (int i = start; i < length; i++) {
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
			return Strings.format("Slot{value={}}",
					value);
		}
	}
}
