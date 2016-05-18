package me.jezza.lava;

/**
 * @author Jezza
 */
public class Main {

	private static final int UPPER_LIMIT = 50;

	public static void main(String[] args) throws Exception {
//		expansionTest("x << 1                ", x -> x << 1);
//		expansionTest("x + (x >> 1)          ", x -> Math.max(x + (x >> 1), 2));
//		expansionTest("x + (x >> 1) + 1      ", x -> x + (x >> 1) + 1);
//		expansionTest("x + (x >> 1)          ", x -> x + (x >> 1));
//		expansionTest("x + 1 + (x + 1 >> 1)  ", x -> x + 1 + (x + 1 >> 1));
//		expansionTest("x * 2 + 1             ", x -> x * 2 + 1);

		try {
			Thread.sleep(100);
		} catch (InterruptedException ignored) {
		}
	}

	public static void expansionTest(String name, IntTransform transform) {
		StringBuilder axis = new StringBuilder();
		StringBuilder values = new StringBuilder();

		for (int i = 0; i < name.length(); i++)
			axis.append(' ');
		values.append(name);
		for (int i = 0; i < 2; i++) {
			axis.append(' ');
			values.append(' ');
		}
		axis.append("| ");
		values.append("| ");

		for (int i = 0; i < UPPER_LIMIT; i++) {
			int t = transform.apply(i);
			values.append(t);
			axis.append(i);

			if (i < UPPER_LIMIT - 1) {
				int transLength = stringSize(t);
				int axisLength = stringSize(i);
				values.append(' ');
				if (transLength >= axisLength) {
					for (int j = 0; j < (transLength + 1 - axisLength); j++)
						axis.append(' ');
				} else {
					for (int j = 0; j < (axisLength + 1 - transLength); j++)
						axis.append(' ');
				}
			}
		}

		System.out.println(axis);
		System.out.println(values);
		System.out.println();
	}

	static final int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE};

	// Requires positive x
	static int stringSize(int x) {
		for (int i = 0; ; i++)
			if (x <= sizeTable[i])
				return i + 1;
	}

	@FunctionalInterface
	public interface IntTransform {
		int apply(int x);
	}

}
