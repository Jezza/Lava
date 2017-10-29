package me.jezza.lava.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import me.jezza.lava.Times;
import me.jezza.lava.lang.base.AbstractLexer;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public final class LavaLexer extends AbstractLexer implements Lexer {
	private static final int BUFFER_SIZE = 2048;

	private final StringBuilder text;

	{
		text = new StringBuilder(32);
	}

	public LavaLexer(String input) {
		super(input, BUFFER_SIZE);
	}

	public LavaLexer(File file) throws FileNotFoundException {
		super(file, BUFFER_SIZE);
	}

	public LavaLexer(InputStream in) {
		super(in, BUFFER_SIZE);
	}

	public LavaLexer(Reader in) {
		super(in, BUFFER_SIZE);
	}

	private static final Times NEXT = new Times("NEXT", 2048);
//	private static final Times WHITESPACE = new Times("WHITESPACE", 2048);
//	private static final Times NAMESPACE = new Times("NAMESPACE", 2048);
//
//	private static final Times EXTRA = new Times("EXTRA", 2048);

	@Override
	public Token next() throws IOException {
		long start = System.nanoTime();
		try {
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
								case "goto":
									return token(Tokens.GOTO, s, pos);
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
									return token(Tokens.NAMESPACE, s, pos);
							}
//						} finally {
//							NAMESPACE.add(System.nanoTime() - startW);
//						}
						}
						return token(c, c, pos);
				}
			}
			return Token.EOS;
		} finally {
			NEXT.add(System.nanoTime() - start);
		}
	}

	private static boolean isAlphabetic(int c) {
		return Character.isUpperCase(c) || Character.isLowerCase(c) || Character.isDigit(c) || c == '_';
	}

//	private static final Times NUMBER = new Times("NUMBER", 2048);

	private Token readNumber(int[] pos, int start) throws IOException {
//		long star2t = System.nanoTime();
//		try {
		StringBuilder text = this.text;
		// Reset buffer
//			if (text.length() > 0)
		text.setLength(0);

		// Prep
		text.append((char) start);
		int c = peek();
		boolean hex = start == '0' && (c == 'x' || c == 'X');
		final int first;
		final int second;
		if (hex) {
			first = 'P';
			second = 'p';
		} else {
			first = 'E';
			second = 'e';
		}
		boolean integer = true;
		// Read number from text
		while (true) {
			if (c == first || c == second) {
				integer = false;
				text.append((char) c);
				advance();
				c = peek();
				if (c == '+' || c == '-')
					text.append((char) c);
			}
			if (Character.isDigit(c)) {
				text.append((char) c);
				advance();
			} else if (c == '.') {
				integer = false;
				text.append((char) c);
				advance();
			} else {
				break;
			}
			c = peek();
		}
		return token(integer ? Tokens.INTEGER : Tokens.FLOAT, text.toString(), pos);
//		} finally {
//			NUMBER.add(System.nanoTime() - star2t);
//		}
	}

//	private static final Times STRING = new Times("STRING", 2048);

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

//	private static final Times LONG_STRING = new Times("LONG_STRING", 2048);

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

//	private static final Times SEP = new Times("SEP", 2048);

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

	protected static Token token(int type, int c, int[] pos) {
		return token(type, String.valueOf((char) c), pos);
	}

	protected static Token token(int type, String text, int[] pos) {
		return new Token(type, text, pos[0], pos[1]);
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Started");
		Lexer lexer = new LavaLexer(new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\test\\resources\\all.lua"));
//		Lexer lexer = new LavaLexer("(1 + 5 + 4)");
//		long ss = System.nanoTime();
//		lexer.next();
//		long ee = System.nanoTime();
//		System.out.println(ee - ss);
		final long start = System.nanoTime();
		int count = 0;
		Token t;
//		long s = System.nanoTime();
		while ((t = lexer.next()) != Token.EOS) {
			count++;
//			long e = System.nanoTime();
//			EXTRA.add(e - s);
//			s = e;
		}
		final long end = System.nanoTime();
//		EXTRA.add(end - s);
		System.out.println(end - start);
		System.out.println("Count: " + count);

		Times.print();
	}
}
