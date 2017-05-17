package me.jezza.lava;

/**
 * @author Jezza
 */
public class GroupedStack {

	// |  type   |        Custom data type        |           Index into typed array
	// [xxxx_xxxx][xxxx_xxxx][xxxx_xxxx][xxxx_xxxx][xxxx_xxxx][xxxx_xxxx][xxxx_xxxx][xxxx_xxxx]

	private int index;
	private long[] indices;

	private int ii;
	private int[] ints;

	private int di;
	private double[] doubles;

	private int oi;
	private Object[] objects;

	public GroupedStack() {
		index = 0;
		indices = new long[0];

		ii = 0;
		ints = new int[0];

		di = 0;
		doubles = new double[0];

		oi = 0;
		objects = new Object[0];
	}

	private void push(byte type, short custom) {
		int length = indices.length;
		if (index >= length) {
			// grow
		}
		// prep
		long value = type;
		value <<= 56;
//		System.out.println(to(value));
		value = value | (((long) custom) << 32);
//		System.out.println(to(value));
		value |= index;
		System.out.println(to(value));
		index++;
	}

	public void push(boolean value) {
		push((byte) 1, (short) (value ? 1 : 0));
	}

	public void push(int value) {
	}

	public void push(double value) {
	}

	public void push(Object value) {
	}

	public static void main(String[] args) {
		GroupedStack stack = new GroupedStack();
		stack.push(true);
		stack.push(false);
		stack.push(true);
	}

	private static String to(long val) {
		int pos = Long.SIZE + 7 + 8;
		char[] buf = new char[pos];
		int count = 1;
		do {
			if (count % 10 == 0) {
				buf[--pos] = ' ';
			} else if (count % 5 == 0) {
				buf[--pos] = '_';
			} else {
				buf[--pos] = ((int) val & 1) != 0 ? '1' : '0';
				val >>>= 1;
			}
			count++;
		} while (pos > 0);
		return new String(buf);
	}
}
