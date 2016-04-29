package me.jezza.luja;

/**
 * @author Jezza
 */
final class MatchState {
	static final char L_ESC = '%';
	static final String SPECIALS = "^$*+?.([%-";

	private static final int CAP_UNFINISHED = -1;
	private static final int CAP_POSITION = -2;

	final Lua L;

	/**
	 * Each capture element is a 2-element array of (index, len).
	 */
	final ChainedList<int[]> capture = new ChainedList<>();

	/**
	 * The entire string that is the subject of the match.
	 */
	final String src;

	/**
	 * The subject's length.
	 */
	final int end;

	/**
	 * Total number of captures (finished or unfinished).
	 */
	int level;

	// :todo: consider adding the pattern string as a member (and removing p parameter from methods).
	// :todo: consider removing end parameter, if end always == // src.length()
	MatchState(Lua L, String src, int end) {
		this.L = L;
		this.src = src;
		this.end = end;
	}

	/**
	 * Returns the init index of capture <var>i</var>.
	 */
	private int captureInit(int i) {
		return capture.get(i)[0];
	}

	/**
	 * Returns the length of capture <var>i</var>.
	 */
	private int captureLen(int i) {
		return capture.get(i)[1];
	}

	/**
	 * Returns the 2-element array for the capture <var>i</var>.
	 */
	private int[] capture(int i) {
		return capture.get(i);
	}

	LuaError invalidCapture() {
		throw L.error("invalid capture index");
	}

	LuaError unfinishedCapture() {
		throw L.error("unfinished capture");
	}

	LuaError malformedBracket() {
		throw L.error("malformed pattern (missing '[')");
	}

	LuaError malformedPattern() {
		throw L.error("malformed pattern (ends with '%')");
	}

	char checkCapture(char l) {
		l -= '1';   // relies on wraparound.
		if (l >= level || captureLen(l) == CAP_UNFINISHED)
			throw invalidCapture();
		return l;
	}

	int captureToClose() {
		int lev = level;
		for (lev--; lev >= 0; lev--)
			if (captureLen(lev) == CAP_UNFINISHED)
				return lev;
		throw invalidCapture();
	}

	int classend(String p, int pi) {
		switch (p.charAt(pi++)) {
			case L_ESC:
				// assert pi < p.length() // checked by callers
				return pi + 1;

			case '[':
				if (p.length() == pi)
					throw malformedBracket();
				if (p.charAt(pi) == '^')
					++pi;
				do    // look for a ']'
				{
					if (p.length() == pi)
						throw malformedBracket();
					if (p.charAt(pi++) == L_ESC) {
						if (p.length() == pi)
							throw malformedBracket();
						++pi;     // skip escapes (e.g. '%]')
						if (p.length() == pi)
							throw malformedBracket();
					}
				} while (p.charAt(pi) != ']');
				return pi + 1;

			default:
				return pi;
		}
	}

	/**
	 * @param c  char match.
	 * @param cl character class.
	 */
	static boolean matchClass(char c, char cl) {
		switch (cl) {
			case 'a':
				return Syntax.isalpha(c);
			case 'A':
				return !Syntax.isalpha(c);
			case 'c':
				return Syntax.iscntrl(c);
			case 'C':
				return !Syntax.iscntrl(c);
			case 'd':
				return Syntax.isdigit(c);
			case 'D':
				return !Syntax.isdigit(c);
			case 'l':
				return Syntax.islower(c);
			case 'L':
				return !Syntax.islower(c);
			case 'p':
				return Syntax.ispunct(c);
			case 'P':
				return !Syntax.ispunct(c);
			case 's':
				return Syntax.isspace(c);
			case 'S':
				return !Syntax.isspace(c);
			case 'u':
				return Syntax.isupper(c);
			case 'U':
				return !Syntax.isupper(c);
			case 'w':
				return Syntax.isalnum(c);
			case 'W':
				return !Syntax.isalnum(c);
			case 'x':
				return Syntax.isxdigit(c);
			case 'X':
				return !Syntax.isxdigit(c);
			case 'z':
			case 'Z':
				return c == 0;
			default:
				return c == cl;
		}
	}

	/**
	 * @param pi index in p of start of class.
	 * @param ec index in p of end of class.
	 */
	static boolean matchBracketClass(char c, String p, int pi, int ec) {
		// :todo: consider changing char c to int c, then -1 could be used
		// represent a guard value at the beginning and end of all strings (a
		// better NUL).  -1 of course would match no positive class.

		// assert p.charAt(pi) == '[';
		// assert p.charAt(ec) == ']';
		boolean sig = true;
		if (p.charAt(pi + 1) == '^') {
			sig = false;
			++pi;     // skip the '6'
		}
		while (++pi < ec) {
			if (p.charAt(pi) == L_ESC) {
				++pi;
				if (matchClass(c, p.charAt(pi)))
					return sig;
			} else if ((p.charAt(pi + 1) == '-') && (pi + 2 < ec)) {
				pi += 2;
				if (p.charAt(pi - 2) <= c && c <= p.charAt(pi))
					return sig;
			} else if (p.charAt(pi) == c) {
				return sig;
			}
		}
		return !sig;
	}

	static boolean singleMatch(char c, String p, int pi, int ep) {
		switch (p.charAt(pi)) {
			case '.':
				return true;    // matches any char
			case L_ESC:
				return matchClass(c, p.charAt(pi + 1));
			case '[':
				return matchBracketClass(c, p, pi, ep - 1);
			default:
				return p.charAt(pi) == c;
		}
	}

	// Generally all the various match functions from PUC-Rio which take a
	// MatchState and return a "const char *" are transformed into
	// instance methods that take and return string indexes.

	int matchBalance(int si, String p, int pi) {
		if (pi + 1 >= p.length())
			throw L.error("Unbalanced pattern");
		if (si >= end || src.charAt(si) != p.charAt(pi))
			return -1;
		char b = p.charAt(pi);
		char e = p.charAt(pi + 1);
		int cont = 1;
		while (++si < end) {
			if (src.charAt(si) == e) {
				if (--cont == 0)
					return si + 1;
			} else if (src.charAt(si) == b) {
				++cont;
			}
		}
		return -1;  // string ends out of balance
	}

	int maxExpand(int si, String p, int pi, int ep) {
		int i = 0;  // counts maximum expand for item
		while (si + i < end && singleMatch(src.charAt(si + i), p, pi, ep))
			++i;
		// keeps trying to match with the maximum repetitions
		while (i >= 0) {
			int res = match(si + i, p, ep + 1);
			if (res >= 0)
				return res;
			--i;      // else didn't match; reduce 1 repetition to try again
		}
		return -1;
	}

	int minExpand(int si, String p, int pi, int ep) {
		while (true) {
			int res = match(si, p, ep + 1);
			if (res >= 0)
				return res;
			else if (si < end && singleMatch(src.charAt(si), p, pi, ep))
				++si;   // try with one more repetition
			else
				return -1;
		}
	}

	int startCapture(int si, String p, int pi, int what) {
		capture.clip(level + 1);
		capture.add(level++, new int[]{si, what});
		int res = match(si, p, pi);
		// match failed
		if (res < 0)
			--level;
		return res;
	}

	int endCapture(int si, String p, int pi) {
		int l = captureToClose();
		capture(l)[1] = si - captureInit(l);        // close it
		int res = match(si, p, pi);
		// match failed?
		if (res < 0)
			capture(l)[1] = CAP_UNFINISHED;   // undo capture
		return res;
	}

	int matchCapture(int si, char l) {
		l = checkCapture(l);
		int len = captureLen(l);
		if (end - si >= len && src.regionMatches(false, captureInit(l), src, si, len))
			return si + len;
		return -1;
	}

	/**
	 * @param si index of subject at which to attempt match.
	 * @param p  pattern string.
	 * @param pi index into pattern (from which to being matching).
	 * @return the index of the end of the match, -1 for no match.
	 */
	int match(int si, String p, int pi) {
		// This code has been considerably changed in the transformation
		// from C to Java.  There are the following non-obvious changes:
		// - The C code routinely relies on NUL being accessible at the end of
		//   the pattern string.  In Java we can't do this, so we use many
		//   more explicit length checks and pull error cases into this
		//   function.  :todo: consider appending NUL to the pattern string.
		// - The C code uses a "goto dflt" which is difficult to transform in
		//   the usual way.
		init:
		// labelled while loop emulates "goto init", which we use to
		// optimize tail recursion.
		while (true) {
			if (p.length() == pi)     // end of pattern
				return si;              // match succeeded
			switch (p.charAt(pi)) {
				case '(':
					if (p.length() == pi + 1)
						throw unfinishedCapture();
					if (p.charAt(pi + 1) == ')')  // position capture?
						return startCapture(si, p, pi + 2, CAP_POSITION);
					return startCapture(si, p, pi + 1, CAP_UNFINISHED);

				case ')':       // end capture
					return endCapture(si, p, pi + 1);

				case L_ESC:
					if (p.length() == pi + 1)
						throw malformedPattern();
					switch (p.charAt(pi + 1)) {
						case 'b':   // balanced string?
							si = matchBalance(si, p, pi + 2);
							if (si < 0)
								return si;
							pi += 4;
							// else return match(ms, s, p+4);
							continue init;    // goto init

						case 'f':   // frontier
						{
							pi += 2;
							if (p.length() == pi || p.charAt(pi) != '[')
								throw L.error("missing '[' after '%f' in pattern");
							int ep = classend(p, pi);   // indexes what is next
							char previous = (si == 0) ? '\0' : src.charAt(si - 1);
							char at = (si == end) ? '\0' : src.charAt(si);
							if (matchBracketClass(previous, p, pi, ep - 1) ||
									!matchBracketClass(at, p, pi, ep - 1)) {
								return -1;
							}
							pi = ep;
							// else return match(ms, s, ep);
						}
						continue init;    // goto init

						default:
							if (Syntax.isdigit(p.charAt(pi + 1))) // capture results (%0-%09)?
							{
								si = matchCapture(si, p.charAt(pi + 1));
								if (si < 0)
									return si;
								pi += 2;
								// else return match(ms, s, p+2);
								continue init;  // goto init
							}
							// We emulate a goto dflt by a fallthrough to the next
							// case (of the outer switch) and making sure that the
							// next case has no effect when we fallthrough to it from here.
							// goto dflt;
					}
					// FALLTHROUGH
				case '$':
					if (p.charAt(pi) == '$') {
						if (p.length() == pi + 1)      // is the '$' the last char in pattern?
							return (si == end) ? si : -1;     // check end of string
						// else goto dflt;
					}
					// FALLTHROUGH
				default:        // it is a pattern item
				{
					int ep = classend(p, pi);   // indexes what is next
					boolean m = si < end && singleMatch(src.charAt(si), p, pi, ep);
					if (p.length() > ep) {
						switch (p.charAt(ep)) {
							case '?':       // optional
								if (m) {
									int res = match(si + 1, p, ep + 1);
									if (res >= 0)
										return res;
								}
								pi = ep + 1;
								// else return match(s, ep+1);
								continue init;      // goto init

							case '*':       // 0 or more repetitions
								return maxExpand(si, p, pi, ep);

							case '+':       // 1 or more repetitions
								return m ? maxExpand(si + 1, p, pi, ep) : -1;

							case '-':       // 0 or more repetitions (minimum)
								return minExpand(si, p, pi, ep);
						}
					}
					// else or default:
					if (!m)
						return -1;
					++si;
					pi = ep;
					// return match(ms, s+1, ep);
					// continue init;
				}
			}
		}
	}

	/**
	 * @param s index of start of match.
	 * @param e index of end of match.
	 */
	Object onecapture(int i, int s, int e) {
		if (i >= level) {
			if (i == 0)       // level == 0, too
				return src.substring(s, e);    // add whole match
			throw invalidCapture();
		}
		int l = captureLen(i);
		if (l == CAP_UNFINISHED)
			throw unfinishedCapture();
		if (l == CAP_POSITION)
			return Double.valueOf(captureInit(i) + 1);
		return src.substring(captureInit(i), captureInit(i) + l);
	}

	void push_onecapture(int i, int s, int e) {
		L.push(onecapture(i, s, e));
	}

	/**
	 * @param s index of start of match.
	 * @param e index of end of match.
	 */
	int push_captures(int s, int e) {
		int nlevels = (level == 0 && s >= 0) ? 1 : level;
		for (int i = 0; i < nlevels; ++i)
			push_onecapture(i, s, e);
		return nlevels;     // number of strings pushed
	}

	/**
	 * A helper for gsub.  Equivalent to add_s from lstrlib.c.
	 */
	void adds(StringBuilder b, int si, int ei) {
		String news = L.toString(L.value(3));
		int l = news.length();
		for (int i = 0; i < l; ++i) {
			if (news.charAt(i) != L_ESC) {
				b.append(news.charAt(i));
			} else {
				++i;    // skip L_ESC
				if (!Syntax.isdigit(news.charAt(i))) {
					b.append(news.charAt(i));
				} else if (news.charAt(i) == '0') {
					b.append(src.substring(si, ei));
				} else {
					// add capture to accumulated result
					b.append(L.toString(onecapture(news.charAt(i) - '1', si, ei)));
				}
			}
		}
	}

	/**
	 * A helper for gsub.  Equivalent to add_value from lstrlib.c.
	 */
	void addvalue(StringBuilder b, int si, int ei) {
		switch (L.type(3)) {
			case Lua.TNUMBER:
			case Lua.TSTRING:
				adds(b, si, ei);
				return;

			case Lua.TFUNCTION: {
				L.pushValue(3);
				int n = push_captures(si, ei);
				L.call(n, 1);
			}
			break;

			case Lua.TTABLE:
				L.push(L.getTable(L.value(3), onecapture(0, si, ei)));
				break;

			default: {
				throw L.argError(3, "string/function/table expected");
			}
		}
		// nil or false
		if (!Lua.toBoolean(L.value(-1))) {
			L.pop(1);
			L.pushString(src.substring(si, ei));
		} else if (!Lua.isString(L.value(-1))) {
			throw L.error("invalid replacement value (a " + Lua.typeName(L.type(-1)) + ")");
		}
		b.append(L.toString(L.value(-1)));  // add result to accumulator
		L.pop(1);
	}
}