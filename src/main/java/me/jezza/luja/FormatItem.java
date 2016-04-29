package me.jezza.luja;

/**
 * @author Jezza
 */
final class FormatItem {
	private Lua L;
	private boolean left; // '-' flag
	private boolean sign; // '+' flag
	private boolean space;        // ' ' flag
	private boolean alt;  // '#' flag
	private boolean zero; // '0' flag
	private int width;    // minimum field width
	private int precision = -1;   // precision, -1 when no precision specified.
	private char type;    // the type of the conversion
	private int length;   // length of the format item in the format string.

	/**
	 * Character used in formatted output when %e or %g format is used.
	 */
	static char E_LOWER = 'E';
	/**
	 * Character used in formatted output when %E or %G format is used.
	 */
	static char E_UPPER = 'E';

	/**
	 * Parse a format item (starting from after the <code>L_ESC</code>).
	 * If you promise that there won't be any format errors, then
	 * <var>L</var> can be <code>null</code>.
	 */
	FormatItem(Lua L, String s) {
		this.L = L;
		int i = 0;
		int l = s.length();
		// parse flags
		flag:
		while (true) {
			if (i >= l)
				throw L.error("Invalid format");
			switch (s.charAt(i)) {
				case '-':
					left = true;
					break;
				case '+':
					sign = true;
					break;
				case ' ':
					space = true;
					break;
				case '#':
					alt = true;
					break;
				case '0':
					zero = true;
					break;
				default:
					break flag;
			}
			++i;
		} /* flag */
		// parse width
		int widths = i;       // index of start of width specifier
		while (true) {
			if (i >= l)
				throw L.error("Invalid format");
			if (Syntax.isdigit(s.charAt(i)))
				++i;
			else
				break;
		}
		if (widths < i) {
			try {
				width = Integer.parseInt(s.substring(widths, i));
			} catch (NumberFormatException ignored) {
			}
		}
		// parse precision
		if (s.charAt(i) == '.') {
			++i;
			int precisions = i; // index of start of precision specifier
			while (true) {
				if (i >= l)
					throw L.error("Invalid format");
				if (Syntax.isdigit(s.charAt(i)))
					++i;
				else
					break;
			}
			if (precisions < i) {
				try {
					precision = Integer.parseInt(s.substring(precisions, i));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		switch (s.charAt(i)) {
			case 'c':
			case 'd':
			case 'i':
			case 'o':
			case 'u':
			case 'x':
			case 'X':
			case 'e':
			case 'E':
			case 'f':
			case 'g':
			case 'G':
			case 'q':
			case 's':
				type = s.charAt(i);
				length = i + 1;
				return;
		}
		throw L.error("Invalid option to 'format'");
	}

	int length() {
		return length;
	}

	int type() {
		return type;
	}

	/**
	 * Format the converted string according to width, and left.
	 * zero padding is handled in either {@link FormatItem#formatInteger}
	 * or {@link FormatItem#formatFloat}
	 * (and width is fixed to 0 in such cases).  Therefore we can ignore
	 * zero.
	 */
	private void format(StringBuilder b, String s) {
		int l = s.length();
		if (l >= width) {
			b.append(s);
			return;
		}
		StringBuilder pad = new StringBuilder();
		while (l < width) {
			pad.append(' ');
			++l;
		}
		if (left) {
			b.append(s);
			b.append(pad);
		} else {
			b.append(pad);
			b.append(s);
		}
	}

	// All the format* methods take a StringBuilder and append the
	// formatted representation of the value to it.
	// Sadly after a format* method has been invoked the object is left in
	// an unusable state and should not be used again.

	void formatChar(StringBuilder b, char c) {
		format(b, String.valueOf(c));
	}

	void formatInteger(StringBuilder b, long i) {
		// :todo: improve inefficient use of implicit StringBuilder

		if (left)
			zero = false;
		if (precision >= 0)
			zero = false;

		int radix;
		switch (type) {
			case 'o':
				radix = 8;
				break;
			case 'd':
			case 'i':
			case 'u':
				radix = 10;
				break;
			case 'x':
			case 'X':
				radix = 16;
				break;
			default:
				throw L.error("Invalid format");
		}
		String s = Long.toString(i, radix);
		if (type == 'X')
			s = s.toUpperCase();
		if (precision == 0 && s.equals("0"))
			s = "";

		// form a prefix by strippping possible leading '-',
		// pad to precision,
		// add prefix,
		// pad to width.
		// extra wart: padding with '0' is implemented using precision
		// because this makes handling the prefix easier.
		String prefix = "";
		if (s.startsWith("-")) {
			prefix = "-";
			s = s.substring(1);
		}
		if (alt && radix == 16)
			prefix = "0x";
		if (prefix == "") {
			if (sign)
				prefix = "+";
			else if (space)
				prefix = " ";
		}
		if (alt && radix == 8 && !s.startsWith("0"))
			s = "0" + s;
		int l = s.length();
		if (zero) {
			precision = width - prefix.length();
			width = 0;
		}
		if (l < precision) {
			StringBuilder p = new StringBuilder(precision - l + s.length());
			while (l < precision) {
				p.append('0');
				++l;
			}
			p.append(s);
			s = p.toString();
		}
		s = prefix + s;
		format(b, s);
	}

	void formatFloat(StringBuilder b, double d) {
		switch (type) {
			case 'g':
			case 'G':
				formatFloatG(b, d);
				return;
			case 'f':
				formatFloatF(b, d);
				return;
			case 'e':
			case 'E':
				formatFloatE(b, d);
		}
	}

	private void formatFloatE(StringBuilder b, double d) {
		format(b, formatFloatRawE(d));
	}

	/**
	 * Returns the formatted string for the number without any padding
	 * (which can be added by invoking {@link FormatItem#format} later).
	 */
	private String formatFloatRawE(double d) {
		double m = Math.abs(d);
		int offset = 0;
		if (m >= 1e-3 && m < 1e7) {
			d *= 1e10;
			offset = 10;
		}

		String s = Double.toString(d);
		StringBuilder t = new StringBuilder(s);
		int e;      // Exponent value
		if (d == 0) {
			e = 0;
		} else {
			int ei = s.indexOf('E');
			e = Integer.parseInt(s.substring(ei + 1));
			t.delete(ei, Integer.MAX_VALUE);
		}

		precisionTrim(t);

		e -= offset;
		if (Character.isLowerCase(type)) {
			t.append(E_LOWER);
		} else {
			t.append(E_UPPER);
		}
		if (e >= 0) {
			t.append('+');
		}
		t.append(Integer.toString(e));

		zeroPad(t);
		return t.toString();
	}

	private void formatFloatF(StringBuilder b, double d) {
		String s = formatFloatRawF(d);
		format(b, s);
	}

	/**
	 * Returns the formatted string for the number without any padding
	 * (which can be added by invoking {@link FormatItem#format} later).
	 */
	private String formatFloatRawF(double d) {
		String s = Double.toString(d);
		StringBuilder t = new StringBuilder(s);

		int di = s.indexOf('.');
		int ei = s.indexOf('E');
		if (ei >= 0) {
			t.delete(ei, Integer.MAX_VALUE);
			int e = Integer.parseInt(s.substring(ei + 1));

			StringBuilder z = new StringBuilder();
			for (int i = 0; i < Math.abs(e); ++i) {
				z.append('0');
			}

			if (e > 0) {
				t.deleteCharAt(di);
				t.append(z);
				t.insert(di + e, '.');
			} else {
				t.deleteCharAt(di);
				int at = t.charAt(0) == '-' ? 1 : 0;
				t.insert(at, z);
				t.insert(di, '.');
			}
		}

		precisionTrim(t);
		zeroPad(t);

		return t.toString();
	}

	private void formatFloatG(StringBuilder b, double d) {
		if (precision == 0) {
			precision = 1;
		}
		if (precision < 0) {
			precision = 6;
		}
		String s;
		// Decide whether to use %e or %f style.
		double m = Math.abs(d);
		if (m == 0) {
			// :todo: Could test for -0 and use "-0" appropriately.
			s = "0";
		} else if (m < 1e-4 || m >= Lua.iNumpow(10, precision)) {
			// %e style
			--precision;
			s = formatFloatRawE(d);
			int di = s.indexOf('.');
			if (di >= 0) {
				// Trim trailing zeroes from fractional part
				int ei = s.indexOf('E');
				if (ei < 0) {
					ei = s.indexOf('e');
				}
				int i = ei - 1;
				while (s.charAt(i) == '0') {
					--i;
				}
				if (s.charAt(i) != '.') {
					++i;
				}
				StringBuilder a = new StringBuilder(s);
				a.delete(i, ei);
				s = a.toString();
			}
		} else {
			// %f style
			// For %g precision specifies the number of significant digits,
			// for %f precision specifies the number of fractional digits.
			// There is a problem because it's not obvious how many fractional
			// digits to format, it could be more than precision
			// (when .0001 <= m < 1) or it could be less than precision
			// (when m >= 1).
			// Instead of trying to work out the correct precision to use for
			// %f formatting we use a worse case to get at least all the
			// necessary digits, then we trim using string editing.  The worst
			// case is that 3 zeroes come after the decimal point before there
			// are any significant digits.
			// Save the required number of significant digits
			int required = precision;
			precision += 3;
			s = formatFloatRawF(d);
			int fsd = 0;      // First Significant Digit
			while (s.charAt(fsd) == '0' || s.charAt(fsd) == '.') {
				++fsd;
			}
			// Note that all the digits to the left of the decimal point in
			// the formatted number are required digits (either significant
			// when m >= 1 or 0 when m < 1).  We know this because otherwise
			// m >= (10**precision) and so formatting falls under the %e case.
			// That means that we can always trim the string at fsd+required
			// (this will remove the decimal point when m >=
			// (10**(precision-1)).
			StringBuilder a = new StringBuilder(s);
			a.delete(fsd + required, Integer.MAX_VALUE);
			if (s.indexOf('.') < a.length()) {
				// Trim trailing zeroes
				int i = a.length() - 1;
				while (a.charAt(i) == '0') {
					a.deleteCharAt(i);
					--i;
				}
				if (a.charAt(i) == '.') {
					a.deleteCharAt(i);
				}
			}
			s = a.toString();
		}
		format(b, s);
	}

	void formatString(StringBuilder b, String s) {
		String p = s;

		if (precision >= 0 && precision < s.length()) {
			p = s.substring(0, precision);
		}
		format(b, p);
	}

	private void precisionTrim(StringBuilder t) {
		if (precision < 0) {
			precision = 6;
		}

		String s = t.toString();
		int di = s.indexOf('.');
		int l = t.length();
		if (0 == precision) {
			t.delete(di, Integer.MAX_VALUE);
		} else if (l > di + precision) {
			t.delete(di + precision + 1, Integer.MAX_VALUE);
		} else {
			for (; l <= di + precision; ++l) {
				t.append('0');
			}
		}
	}

	private void zeroPad(StringBuilder t) {
		if (zero && t.length() < width) {
			int at = t.charAt(0) == '-' ? 1 : 0;
			while (t.length() < width) {
				t.insert(at, '0');
			}
		}
	}
}
