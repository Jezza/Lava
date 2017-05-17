package me.jezza.lava;

import sun.misc.Unsafe;

/**
 * @author Jezza
 */
final class UnsafeStack {
	private static final Unsafe UNSAFE = Bypass.UNSAFE;

	private static final byte BOOLEAN_TAIL = 1;
	private static final int BOOLEAN_SIZE = 8;

	private static final byte INTEGER_TAIL = 2;
	private static final int INTEGER_SIZE = 32;

	private static final byte DOUBLE_TAIL = 3;
	private static final byte DOUBLE_SIZE = 64;

	private static final byte OBJECT_TAIL = 4;

	private static final byte KEYFRAME_TAIL = 5;

	private static final byte TAIL_SIZE = 8;

	private long capacity;
	private long size;
	private long base;

	private long current;

	private Object[] data;
	private int index;

	UnsafeStack(long initialSize) {
		if (initialSize <= 0)
			throw new IllegalStateException("Size cannot be 0 or negative.");
		current = base = UNSAFE.allocateMemory(initialSize);
		capacity = initialSize;
		size = 0;
		data = new Object[0];
		index = 0;
	}

	void push(boolean value) {
		// 1
		grow(BOOLEAN_SIZE);
		UNSAFE.putByte(current, (byte) (value ? 1 : 0));
		current += BOOLEAN_SIZE;
		pushTail(BOOLEAN_TAIL);
	}

	void push(int value) {
		// 32
		grow(INTEGER_SIZE);
		UNSAFE.putInt(current, value);
		current += INTEGER_SIZE;
		pushTail(INTEGER_TAIL);
	}

	void push(double value) {
		// 64
		grow(DOUBLE_SIZE);
		UNSAFE.putDouble(current, value);
		current += DOUBLE_SIZE;
		pushTail(DOUBLE_TAIL);
	}

	void push(Object value) {
		// 32
		if (index == data.length) {
			int oldLength = data.length;
			int newLength = oldLength + 1 + (oldLength + 1 >> 1);
			Object[] newData = new Object[newLength];
			System.arraycopy(data, 0, newData, 0, oldLength);
			data = newData;
		}
		int i = index++;
		data[i] = value;
		pushTail(OBJECT_TAIL);
	}

	private long findTail(int index) {
		if (index < 0)
			throw new IllegalArgumentException("Index cannot be negative.");
		if (size <= index)
			throw new IllegalArgumentException("Illegal index: " + index + ", size: " + size);
		if (index == 0)
			return current - TAIL_SIZE;
		long pos = current;
		for (int i = 0; i < index; i++) {
			pos -= TAIL_SIZE;
			switch (UNSAFE.getByte(pos)) {
				case BOOLEAN_TAIL:
					pos -= BOOLEAN_SIZE;
					continue;
				case INTEGER_TAIL:
					pos -= INTEGER_SIZE;
					continue;
				case DOUBLE_TAIL:
					pos -= DOUBLE_SIZE;
					continue;
				case OBJECT_TAIL:
					continue;
			}
		}
		return pos - TAIL_SIZE;
	}

	void mutateBoolean(int index, BooleanMutator mutator) {
		long pos = findTail(index);
		assertTail(pos, BOOLEAN_TAIL);
		pos -= BOOLEAN_SIZE;
		UNSAFE.putByte(pos, (byte) (mutator.mutateBoolean(UNSAFE.getByte(pos) == 1) ? 1 : 0));
	}

	public interface BooleanMutator {
		boolean mutateBoolean(boolean value);
	}

	void mutateInt(int index, IntMutator mutator) {
		long pos = findTail(index);
		assertTail(pos, INTEGER_TAIL);
		pos -= INTEGER_SIZE;
		UNSAFE.putInt(pos, mutator.mutateInt(UNSAFE.getInt(pos)));
	}

	public interface IntMutator {
		int mutateInt(int value);
	}

	boolean popBoolean() {
		shrink(TAIL_SIZE);
		assertTail(current, BOOLEAN_TAIL);
		shrink(BOOLEAN_SIZE);
		return UNSAFE.getByte(current) == 1;
	}

	int popInt() {
		shrink(TAIL_SIZE);
		assertTail(current, INTEGER_TAIL);
		shrink(INTEGER_SIZE);
		size--;
		return UNSAFE.getInt(current);
	}

	double popDouble() {
		shrink(TAIL_SIZE);
		assertTail(current, DOUBLE_TAIL);
		shrink(DOUBLE_SIZE);
		size--;
		return UNSAFE.getDouble(current);
	}

	Object popObject() {
		shrink(TAIL_SIZE);
		assertTail(current, OBJECT_TAIL);
		if (size-- == 0 || index-- == 0)
			throw new IllegalStateException("Illegal stack state. (Expected object on top, was empty)");
		return data[index];
	}

	private void grow(long amount) {
		long diff = current - base;
		long needed = diff + amount;
		if (needed > capacity) {
			long newCapacity = next(needed);
			long newBase = UNSAFE.allocateMemory(newCapacity);
			UNSAFE.copyMemory(base, newBase, capacity + 1);
			UNSAFE.freeMemory(base);
			base = newBase;
			capacity = newCapacity;
			current = newBase + diff;
		}
	}

	private void shrink(long amount) {
		// TODO: 03/03/2017 Add shrink capability.
		current -= amount;
	}

	private void pushTail(byte value) {
		grow(TAIL_SIZE);
		UNSAFE.putByte(current, value);
		current += TAIL_SIZE;
		size++;
	}

	private void assertTail(long pos, byte value) {
		byte tail = UNSAFE.getByte(pos);
		if (tail != value)
			throw new IllegalStateException("Illegal tail: g: " + tail + ", e: " + value);
	}

	private static final long INCREMENT_BOUNDARY = 0x200;

	private static long next(long needed) {
		return needed >= INCREMENT_BOUNDARY ? (needed & ~(INCREMENT_BOUNDARY - 1)) + INCREMENT_BOUNDARY : Long.highestOneBit(needed) << 1;
	}

	public static void main(String[] args) {
		UnsafeStack stack = new UnsafeStack(1);
		stack.push(5);
		System.out.println(stack.popInt());

		long start = System.nanoTime();
		stack.push(true);
		long end = System.nanoTime();
		System.out.println(end - start);

		BooleanMutator mutator = in -> !in;

		start = System.nanoTime();
		stack.mutateBoolean(0, mutator);
		end = System.nanoTime();
		System.out.println(end - start);

		start = System.nanoTime();
		stack.mutateBoolean(0, mutator);
		end = System.nanoTime();
		System.out.println(end - start);

		start = System.nanoTime();
		stack.mutateBoolean(0, mutator);
		end = System.nanoTime();
		System.out.println(end - start);

		start = System.nanoTime();
		stack.mutateBoolean(0, mutator);
		end = System.nanoTime();
		System.out.println(end - start);

		start = System.nanoTime();
		boolean b = stack.popBoolean();
		end = System.nanoTime();
		System.out.println(end - start);
		System.out.println(b);

		stack.push(5);
		stack.mutateInt(0, in -> in + 5);
		System.out.println(stack.popInt());

		System.out.println("%%%");
		boolean[] array = new boolean[1];
		array[0] = args.length == 0;
		start = System.nanoTime();
		end = System.nanoTime();
		System.out.println(end - start);

		stack.push(4D);
		stack.popDouble();

		int n = 100;
		for (int i = 0; i < n; i++) {
			stack.push(Integer.toString(i, 16));
		}
		for (int i = 0; i < n; i++) {
			System.out.println(stack.popObject());
		}

		stack.push(21D);
		double v = stack.popDouble();
		System.out.println(v);

		stack.push(true);
		stack.push(true);
		stack.push(false);
		stack.push(true);
		stack.push(51.58185D);
		stack.push(false);
		stack.push(true);
		stack.push(432);
		stack.push(781);
		stack.push(3138);

		System.out.println(stack.popInt());
		System.out.println(stack.popInt());
		System.out.println(stack.popInt());
		System.out.println(stack.popBoolean());
		System.out.println(stack.popBoolean());
		System.out.println(stack.popDouble());
		System.out.println(stack.popBoolean());
		System.out.println(stack.popBoolean());
		System.out.println(stack.popBoolean());
		System.out.println(stack.popBoolean());


		int count = 50;
		for (int i = 0; i < count; i++)
			stack.push((double) i);
		for (int i = 0; i < count; i++)
			System.out.println(stack.popDouble());


	}


}
