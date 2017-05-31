package me.jezza.lava.lang;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import me.jezza.lava.Context;
import me.jezza.lava.Lua;
import me.jezza.lava.lang.ast.Expression;
import me.jezza.lava.lang.ast.FunctionScope;
import me.jezza.lava.lang.base.AbstractParser;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public final class LavaParser extends AbstractParser {

	private final Lua L;
	private final Context context;
	private final String source;

	private FunctionScope active;

	protected LavaParser(Lua L, Lexer lexer, String source) {
		super(lexer);
		this.L = L;
		context = new Context(L);
		this.source = source;
	}

	public void start() throws IOException {
		FunctionScope scope = openScope();
		scope.set(FunctionScope.VARARGS, true);
		chunk();
	}

	public FunctionScope openScope() {
		return active = new FunctionScope(active);
	}

	public void chunk() throws IOException {
		// chunk -> { stat [';'] }
		boolean last = false;
		context.enterLevel();
		while (!last && blockFollowing(current())) {
			last = statement(current());
			match(';');
//			active.
		}
		context.leaveLevel();
	}

	private static boolean blockFollowing(Token token) {
		switch (token.type) {
			case Tokens.ELSE:
			case Tokens.ELSEIF:
			case Tokens.END:
			case Tokens.UNTIL:
			case Tokens.EOS:
				return true;
			default:
				return false;
		}
	}

	private boolean statement(Token current) throws IOException {
		switch (current.type) {
			case Tokens.IF:
				parseIf(current);
				return false;
			case Tokens.WHILE:
				return false;
			case Tokens.DO:
				return false;
			case Tokens.FOR:
				return false;
			case Tokens.REPEAT:
				return false;
			case Tokens.FUNCTION:
				return false;
			case Tokens.LOCAL:
				return false;
			case Tokens.RETURN:
				return true;
			case Tokens.BREAK:
				return true;
			default:
				return false;
		}
	}

	private void parseIf(Token current) throws IOException {

	}

	private void parseThen(Token current) throws IOException {
		consume();
	}

	public void parseWhile() throws IOException {
		parseWhile(current());
	}

	private void parseWhile(Token current) throws IOException {
	}

	private static final Map<Integer, Rules> rules = new HashMap<>();

	static class Rules {
		private final Prefix prefix;
		private final Infix infix;
		private final int precedence;

		public Rules(Prefix prefix, Infix infix, int precedence) {
			this.prefix = prefix;
			this.infix = infix;
			this.precedence = precedence;
		}
	}

	interface Prefix {
		Expression parse(LavaParser parser);
	}

	interface Infix {
		Expression parse(LavaParser parser, Expression left);
	}

	public Expression parseExpression() throws IOException {
		return parseExpression(0);
	}

	private Expression parseExpression(int level) throws IOException {
		Token current = consume();
		Rules r = rules.get(current.type);
		if (r == null)
			throw new IllegalStateException("Unknown starting expression: " + current);
		Expression left = r.prefix.parse(this);
		while (level < (r = rules.get(current().type)).precedence) {
			consume();
			r.infix.parse(this, left);
		}
		return left;
	}


	public static void main(String[] args) throws IOException {
		LavaLexer lexer = new LavaLexer("1 + 5 + 4");
		Lua L = new Lua();
		LavaParser testing = new LavaParser(L, lexer, "Testing");

		Expression expression = testing.parseExpression();

	}
}
