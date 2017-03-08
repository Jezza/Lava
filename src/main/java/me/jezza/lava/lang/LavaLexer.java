package me.jezza.lava.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;

import me.jezza.lava.Times;
import me.jezza.lava.lang.base.AbstractLexer;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public final class LavaLexer extends AbstractLexer implements Lexer {
	private static final int DEFAULT_LOOKAHEAD = 5_000;
//	private static final int DEFAULT_LOOKAHEAD = 1;

	private final StringBuilder text;

	{
		text = new StringBuilder(32);
	}

	protected LavaLexer(String input) {
		super(input, DEFAULT_LOOKAHEAD);
	}

	protected LavaLexer(File file) throws FileNotFoundException {
		super(file, DEFAULT_LOOKAHEAD);
	}

	protected LavaLexer(InputStream in) {
		super(in, DEFAULT_LOOKAHEAD);
	}

	protected LavaLexer(Reader in) {
		super(in, DEFAULT_LOOKAHEAD);
	}

	private static final Times NEXT = new Times("NEXT", 2048);
	private static final Times WHITESPACE = new Times("WHITESPACE", 2048);
	private static final Times NAMESPACE = new Times("NAMESPACE", 2048);

	private static final Times EXTRA = new Times("EXTRA", 2048);

	@Override
	public Token next() throws IOException {
//		long start = System.nanoTime();
//		try {
		int c;
		int[] pos = this.pos.clone();
		while ((c = advance()) != EOS) {
			switch (c) {
				case '-': {
					if (peek() != '-')
						return token('-', c, pos);
					advance();
					if (peek() != '[') {
						// Line comment, scan until EOS or EOL
						while ((c = advance()) != EOS && c != '\n') ;
						pos = this.pos.clone();
						continue;
					}
					// block comment
					int count = skipSeparator(advance());
					readLongString(pos, false, count);
					pos = this.pos.clone();
					continue;
				}
				case '[': {
					int count = skipSeparator(c);
					if (count < 0)
						return token('[', '[', pos);
					return readLongString(pos, true, count);
				}
				case '=': {
					if (peek() != '=')
						return token('=', '=', pos);
					advance();
					return token(Tokens.EQ, "==", pos);
				}
				case '<': {
					if (peek() != '=')
						return token('<', '<', pos);
					advance();
					return token(Tokens.LE, "<=", pos);
				}
				case '>': {
					if (peek() != '=')
						return token('>', '>', pos);
					advance();
					return token(Tokens.GE, ">=", pos);
				}
				case '~': {
					if (peek() != '=')
						return token('~', '~', pos);
					advance();
					return token(Tokens.NE, "~=", pos);
				}
				case '"':
				case '\'': {
					return readString(pos, c);
				}
				case '.': {
					int p = peek();
					if (p == '.') {
						advance();
						if (peek() == '.')
							return token(Tokens.DOTS, "...", pos);
						return token(Tokens.CONCAT, "..", pos);
					} else if (!Character.isDigit(p)) {
						return token('.', '.', pos);
					}
					return readNumber(pos, c);
				}
				default:
					if (Character.isWhitespace(c)) {
//						long startW = System.nanoTime();
						while (Character.isWhitespace(peek()))
							advance();
						pos = this.pos.clone();
//						WHITESPACE.add(System.nanoTime() - startW);
						continue;
					}
					if (Character.isDigit(c))
						return readNumber(pos, c);
					if (isAlphabetic(c)) {
//						long startW = System.nanoTime();
//						try {
						StringBuilder text = this.text;
//								if (text.length() > 0)
						text.setLength(0);
						text.append((char) c);
						while (isAlphabetic(peek()))
							text.append((char) advance());
						String s = text.toString();
						switch (s) {
							case "and":
								return token(Tokens.AND, s, pos);
							case "break":
								return token(Tokens.BREAK, s, pos);
							case "do":
								return token(Tokens.DO, s, pos);
							case "else":
								return token(Tokens.ELSE, s, pos);
							case "elseif":
								return token(Tokens.ELSEIF, s, pos);
							case "end":
								return token(Tokens.END, s, pos);
							case "false":
								return token(Tokens.FALSE, s, pos);
							case "for":
								return token(Tokens.FOR, s, pos);
							case "function":
								return token(Tokens.FUNCTION, s, pos);
							case "if":
								return token(Tokens.IF, s, pos);
							case "in":
								return token(Tokens.IN, s, pos);
							case "local":
								return token(Tokens.LOCAL, s, pos);
							case "nil":
								return token(Tokens.NIL, s, pos);
							case "not":
								return token(Tokens.NOT, s, pos);
							case "or":
								return token(Tokens.OR, s, pos);
							case "repeat":
								return token(Tokens.REPEAT, s, pos);
							case "return":
								return token(Tokens.RETURN, s, pos);
							case "then":
								return token(Tokens.THEN, s, pos);
							case "true":
								return token(Tokens.TRUE, s, pos);
							case "until":
								return token(Tokens.UNTIL, s, pos);
							case "while":
								return token(Tokens.WHILE, s, pos);
							default:
								return token(Tokens.NAME, s, pos);
						}
//						} finally {
//							NAMESPACE.add(System.nanoTime() - startW);
//						}
					}
					return token(c, c, pos);
			}
		}
		return Token.EOS;
//		} finally {
//			NEXT.add(System.nanoTime() - start);
//		}
	}

	private static boolean isAlphabetic(int c) {
		return Character.isAlphabetic(c) || c == '_';
	}

	private static final Times NUMBER = new Times("NUMBER", 2048);

	private Token readNumber(int[] pos, int start) throws IOException {
//		if (pos.length > 1) {
//			return token(Tokens.FLT, "1.1", pos);
//		}
//		long start2 = System.nanoTime();
//		try {
		StringBuilder text = this.text;
		// Reset buffer
//			if (text.length() > 0)
		text.setLength(0);

		// Prep
		text.append((char) start);
		int c = peek();
		boolean hex = start == '0' && (c == 'x' || c == 'X');
		int first = hex ? 'P' : 'E';
		int second = hex ? 'p' : 'e';
		// Read number from text
		while (true) {
//			if (hex ? c == 'P' || c == 'p' : c == 'E' || c == 'e') {
			if (c == first || c == second) {
				text.append((char) c);
				advance();
				c = peek();
				if (c == '+' || c == '-')
					text.append((char) c);
			}
			if (Character.isDigit(c) || c == '.') {
				text.append((char) c);
				advance();
			} else {
				break;
			}
			c = peek();
		}
//		String number = text.toString();
//		final double v;
//		try {
//			v = Double.parseDouble(number);
//		} catch (NumberFormatException e) {
//			throw new IllegalStateException("Malformed number near: " + Arrays.toString(pos));
//		}
//		if (isWhole(v)) {
//			int whole = (int) v;
//			// TODO: 06/03/2017 Use integer value in token
//			return token(Tokens.INT, text.toString(), pos);
//		}
//		// TODO: 06/03/2017 Use double value in token
//		return token(Tokens.FLT, text.toString(), pos);
//		} finally {
//			NUMBER.add(System.nanoTime() - start2);
//		}
		return number(text.toString());
	}

	private Token number(String number) {
		final double v;
		try {
			v = Double.parseDouble(number);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Malformed number near: " + Arrays.toString(pos));
		}
		if (isWhole(v)) {
			int whole = (int) v;
			// TODO: 06/03/2017 Use integer value in token
			return token(Tokens.INT, text.toString(), pos);
		}
		// TODO: 06/03/2017 Use double value in token
		return token(Tokens.FLT, text.toString(), pos);
	}


	private static boolean isWhole(double value) {
		return !Double.isNaN(value) && !Double.isInfinite(value) && value == Math.rint(value);
	}

	private static final Times STRING = new Times("STRING", 2048);

	private Token readString(int[] pos, int style) throws IOException {
//		long start = System.nanoTime();
//		try {
		StringBuilder text = this.text;
//			if (text.length() > 0)
		text.setLength(0);
		int c;
		while ((c = advance()) != EOS) {
			switch (c) {
				case '\\': {
					int e;
					switch (e = advance()) {
						case 'a':
							text.append((char) 7);
							continue;
						case 'b':
							text.append('\b');
							continue;
						case 'f':
							text.append('\f');
							continue;
						case 'n':
							text.append('\n');
							continue;
						case 'r':
							text.append('\r');
							continue;
						case 't':
							text.append('\t');
							continue;
						case 'v':
							text.append((char) 11);
							continue;
						default:
							if (!Character.isDigit(e)) {
								text.append((char) e); // handles \\, \", \', \?
							} else { // \xxx
								int i = 0;
								c = 0;
								do {
									c = 10 * c + (e - '0');
									e = peek();
								} while (++i < 3 && Character.isDigit(e = advance()));
								// In unicode, there are no bounds on a 3-digit decimal.
								text.append((char) c);
							}
							continue;
					}
				}
				case '\r':
				case '\n':
					throw new IllegalArgumentException("Illegal line end on string literal");
				case '"':
				case '\'':
					if (c == style)
						return token(Tokens.STRING, text.toString(), pos);
				default:
					text.append((char) c);
			}
		}
		throw new IllegalArgumentException("Illegal file end on string literal");
//		} finally {
//			STRING.add(System.nanoTime() - start);
//		}
	}

	private static final Times LONG_STRING = new Times("LONG_STRING", 2048);

	private Token readLongString(int[] pos, boolean isString, int count) throws IOException {
		StringBuilder text = this.text;
		advance();
		if (isString) //  && text.length() > 0
			text.setLength(0);
//		long start = System.nanoTime();
//		try {
		int c;
		while ((c = advance()) != EOS) {
			if (c == ']') {
				if (skipSeparator(c) == count) {
					advance();
					return isString ? token(Tokens.STRING, text.toString(), pos) : null;
				}
			} else if (isString)
				text.append((char) c);
		}
		throw new IllegalArgumentException(isString ? "Illegal file end on string literal" : "Illegal file end in comment");
//		} finally {
//			LONG_STRING.add(System.nanoTime() - start);
//		}
	}

	private static final Times SEP = new Times("SEP", 2048);

	private int skipSeparator(int expecting) throws IOException {
//		long start = System.nanoTime();
//		try {
		int count = 0;
		int s;
		while ((s = peek()) == '=') {
			advance();
			count++;
		}
		return expecting == s ? count : -count - 1;
//		} finally {
//			SEP.add(System.nanoTime() - start);
//		}
	}

	protected final Token token(int type, int c, int[] pos) {
		return token(type, String.valueOf((char) c), pos);
	}

	protected final Token token(int type, String text, int[] pos) {
		return new Token(type, text, pos[0], pos[1]);
	}

	public static void main(String[] args) throws IOException {
//		Lexer lexer = new LavaLexer(new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\test\\resources\\all.lua"));
		long start = System.nanoTime();
		Lexer lexer = new LavaLexer("(1+5+4)");
		Token t;
		long s = System.nanoTime();
		while ((t = lexer.next()) != Token.EOS) {
			long e = System.nanoTime();
			EXTRA.add(e - s);
			s = e;
		}
		long end = System.nanoTime();
		EXTRA.add(end - s);
		System.out.println(end - start);

		Times.print();
	}
}