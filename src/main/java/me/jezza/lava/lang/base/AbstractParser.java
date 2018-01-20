package me.jezza.lava.lang.base;

import java.io.IOException;

import me.jezza.lava.Strings;
import me.jezza.lava.lang.Token;
import me.jezza.lava.lang.Tokens;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public abstract class AbstractParser {
	private final Lexer lexer;

	private Token current;

	protected AbstractParser(Lexer lexer) {
		if (lexer == null)
			throw new NullPointerException("Lexer cannot be null.");
		this.lexer = lexer;
	}

	public final Token current() throws IOException {
			return current != null
					? current
					: (current = lexer.next());
	}

	public final boolean match(int type) throws IOException {
		boolean match = current().type == type;
		if (match)
			current = null;
		return match;
	}

	public final boolean is(int type) throws IOException {
			return current().type == type;
	}

	public final Token consume() throws IOException {
		Token token = current();
		current = null;
		return token;
	}

	public final Token consume(int type) throws IOException {
		Token token = current();
		if (token.type == type) {
			current = null;
			return token;
		}
		throw new RuntimeException(Strings.format("Unexpected token {}, expected {}.", token, Tokens.name(type)));
	}
}
