package me.jezza.lava.utils;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import me.jezza.lava.Strings;

/**
 * @author Jezza
 */
public final class Numbers {
	private Numbers() {
		throw new IllegalStateException();
	}

	public static OptionalInt parseInt(String value) {
		return parseInt(value, 10);
	}

	public static OptionalInt parseInt(String value, int radix) {
		if (!Strings.useable(value) || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
			return OptionalInt.empty();
		}
		int result = 0;
		boolean negative = false;
		int i = 0;
		int len = value.length();
		int limit = -Integer.MAX_VALUE;
		int multmin;
		int digit;

		if (len > 0) {
			char firstChar = value.charAt(0);
			if (firstChar < '0') { // Possible leading "+" or "-"
				if (firstChar == '-') {
					negative = true;
					limit = Integer.MIN_VALUE;
				} else if (firstChar != '+') {
					return OptionalInt.empty();
				}

				if (len == 1) {
					return OptionalInt.empty();
				}
				i++;
			}
			multmin = limit / radix;
			while (i < len) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(value.charAt(i++), radix);
				if (digit < 0) {
					return OptionalInt.empty();
				}
				if (result < multmin) {
					return OptionalInt.empty();
				}
				result *= radix;
				if (result < limit + digit) {
					return OptionalInt.empty();
				}
				result -= digit;
			}
		} else {
			return OptionalInt.empty();
		}
		return OptionalInt.of(negative ? result : -result);
	}

	public static OptionalLong parseLong(String value) {
		return parseLong(value, 10);
	}

	public static OptionalLong parseLong(String s, int radix) {
		if (!Strings.useable(s) || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
			return OptionalLong.empty();
		}
		long result = 0;
		boolean negative = false;
		int i = 0;
		int len = s.length();
		long limit = -Long.MAX_VALUE;
		long multmin;
		int digit;

		if (len > 0) {
			char firstChar = s.charAt(0);
			if (firstChar < '0') { // Possible leading "+" or "-"
				if (firstChar == '-') {
					negative = true;
					limit = Long.MIN_VALUE;
				} else if (firstChar != '+') {
					return OptionalLong.empty();
				}

				if (len == 1) {
					return OptionalLong.empty();
				}
				i++;
			}
			multmin = limit / radix;
			while (i < len) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(s.charAt(i++), radix);
				if (digit < 0) {
					return OptionalLong.empty();
				}
				if (result < multmin) {
					return OptionalLong.empty();
				}
				result *= radix;
				if (result < limit + digit) {
					return OptionalLong.empty();
				}
				result -= digit;
			}
		} else {
			return OptionalLong.empty();
		}
		return OptionalLong.of(negative ? result : -result);
	}

	public static OptionalDouble parseDouble(String value) {
		if (!Strings.useable(value)) {
			return OptionalDouble.empty();
		}
		try {
			return OptionalDouble.of(Double.parseDouble(value));
		} catch (NumberFormatException e) {
			return OptionalDouble.empty();
		}
	}
}