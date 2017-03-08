package me.jezza.lava.lang.base;

import java.io.IOException;

import me.jezza.lava.Strings;
import me.jezza.lava.Times;
import me.jezza.lava.lang.Token;
import me.jezza.lava.lang.Tokens;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public abstract class AbstractParser {
	private final Lexer lexer;

	private Token current;

	protected AbstractParser(final Lexer lexer) {
		if (lexer == null)
			throw new NullPointerException("Lexer cannot be null.");
		this.lexer = lexer;
		current = null;
	}

	private static final Times CURRENT = new Times("current", 411);

	public final Token current() throws IOException {
		long start = System.nanoTime();
		try {
			return current != null ? current : (current = lexer.next());
		} finally {
			CURRENT.add(System.nanoTime() - start);
		}
	}

	private static final Times MATCH = new Times("match", 174);

	public final boolean match(int type) throws IOException {
		long start = System.nanoTime();
		boolean match = current().type == type;
		if (match)
			current = null;
		MATCH.add(System.nanoTime() - start);
		return match;
	}

	private static final Times LOOKAHEAD = new Times("lookAhead", 22);

	public final boolean lookAhead(int type) throws IOException {
		long start = System.nanoTime();
		try {
			return current().type == type;
		} finally {
			LOOKAHEAD.add(System.nanoTime() - start);
		}
	}

	private static final Times CONSUME = new Times("consume", 117);

	public final Token consume() throws IOException {
		long start = System.nanoTime();
		Token token = current();
		current = null;
		CONSUME.add(System.nanoTime() - start);
		return token;
	}

	private static final Times CONSUME_INT = new Times("consume(INT)", 52);

	public final Token consume(int type) throws IOException {
		long start = System.nanoTime();
		Token token = current();
		if (token.type == type) {
			current = null;
			CONSUME_INT.add(System.nanoTime() - start);
			return token;
		}
		throw new RuntimeException(Strings.format("Expected token {}, found {}.", Tokens.name(type), token));
	}
}
