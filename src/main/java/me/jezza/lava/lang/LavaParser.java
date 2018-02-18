package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.FLAG_ASSIGNMENT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_UNCHECKED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import me.jezza.lava.lang.ParseTree.Assignment;
import me.jezza.lava.lang.ParseTree.BinaryOp;
import me.jezza.lava.lang.ParseTree.Block;
import me.jezza.lava.lang.ParseTree.Break;
import me.jezza.lava.lang.ParseTree.DoBlock;
import me.jezza.lava.lang.ParseTree.Expression;
import me.jezza.lava.lang.ParseTree.ExpressionList;
import me.jezza.lava.lang.ParseTree.ForList;
import me.jezza.lava.lang.ParseTree.ForLoop;
import me.jezza.lava.lang.ParseTree.FunctionBody;
import me.jezza.lava.lang.ParseTree.FunctionCall;
import me.jezza.lava.lang.ParseTree.Goto;
import me.jezza.lava.lang.ParseTree.IfBlock;
import me.jezza.lava.lang.ParseTree.Label;
import me.jezza.lava.lang.ParseTree.Literal;
import me.jezza.lava.lang.ParseTree.Name;
import me.jezza.lava.lang.ParseTree.RepeatBlock;
import me.jezza.lava.lang.ParseTree.ReturnStatement;
import me.jezza.lava.lang.ParseTree.Statement;
import me.jezza.lava.lang.ParseTree.TableConstructor;
import me.jezza.lava.lang.ParseTree.TableField;
import me.jezza.lava.lang.ParseTree.UnaryOp;
import me.jezza.lava.lang.ParseTree.Varargs;
import me.jezza.lava.lang.ParseTree.WhileLoop;
import me.jezza.lava.lang.base.AbstractParser;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public final class LavaParser extends AbstractParser {
	public LavaParser(Lexer lexer) {
		super(lexer);
	}

	public Block chunk() throws IOException {
		Block block = block();
		block.parameterCount = Block.VARARGS;
		if (!match(Tokens.EOS))
			System.out.println("File should have ended: " + current());
		return block;
	}

	public Block block() throws IOException {
		List<Statement> statements = new ArrayList<>();
		Statement statement;
		while (!blockFollowing(current().type)) {
			statement = statement();
			if (statement == null) {
				continue;
			}
			statements.add(statement);
			if (statement instanceof Break || statement instanceof ReturnStatement) {
				break;
			}
		}
		return new Block("root", statements);
	}

	public Label label() throws IOException {
		consume(':');
		consume(':');
		String name = name();
		consume(':');
		consume(':');
		return new Label(name);
	}

	public Statement statement() throws IOException {
		switch (current().type) {
			case Tokens.DO:
				return doBlock();
			case Tokens.LOCAL:
				return local();
			case Tokens.FUNCTION:
				return functionStatement();
			case Tokens.GOTO:
				return gotoStatement();
			case Tokens.REPEAT:
				return repeatBlock();
			case Tokens.IF:
				return ifBlock();
			case Tokens.WHILE:
				return whileLoop();
			case Tokens.FOR:
				return forLoop();
			case Tokens.BREAK:
				consume();
				while (match(';'));
				return new Break();
			case Tokens.RETURN:
				consume();
				// @CLEANUP Jezza - 20 Jan 2018: This is a bit messy...
				ExpressionList expressions = blockFollowing(current().type) || match(';')
						? new ExpressionList(new ArrayList<>())
						: expressionList();
				match(';');
				return new ReturnStatement(expressions);
			case ':':
				return label();
			case ';':
				consume();
				return null;
			default:
				return expressionStatement();
		}
	}

	public Statement local() throws IOException {
		consume(Tokens.LOCAL);
		if (match(Tokens.FUNCTION)) {
			Name name = new Name(name(), FLAG_LOCAL | FLAG_ASSIGNMENT);
			FunctionBody functionBody = functionBody();
			return new Assignment(name, functionBody);
		}
		List<Expression> names = new ArrayList<>();
		do {
			names.add(new Name(name(), FLAG_LOCAL | FLAG_ASSIGNMENT));
		} while (match(','));
		ExpressionList lhs = new ExpressionList(names);
		ExpressionList rhs;
		int size = names.size();
		if (match('=')) {
			rhs = expressionList(size);
		} else {
			assert size > 0;
			List<Expression> values = new ArrayList<>(size);
			do {
				values.add(new Literal(Literal.NIL, null));
			} while(--size > 0);
			rhs = new ExpressionList(values);
		}
		return new Assignment(lhs, rhs);
	}

	public Statement functionStatement() throws IOException {
		consume(Tokens.FUNCTION);
		Expression left = new Name(name(), FLAG_UNCHECKED);
		while (match('.')) {
			left = new BinaryOp(BinaryOp.OP_INDEXED, left, new Literal(Literal.STRING, name()));
		}
		boolean self = match(':');
		if (self) {
			left = new BinaryOp(BinaryOp.OP_INDEXED, left, new Literal(Literal.STRING, name()));
		}
		left.set(FLAG_ASSIGNMENT);
		FunctionBody functionBody = functionBody();
		if (self) {
			functionBody.parameters.add(0, new Name("self", FLAG_LOCAL));
		}
		return new Assignment(left, functionBody);
	}

	private FunctionBody functionBody() throws IOException {
		consume('(');
		List<Name> parameters = new ArrayList<>();
		boolean varargs = false;
		if (!match(')')) {
			do {
				if (match(Tokens.DOTS)) {
					varargs = true;
					break;
				}
				parameters.add(new Name(name(), FLAG_LOCAL));
			} while (match(','));
			consume(')');
		}
		Block body = block();
		body.parameterCount = parameters.size();
		consume(Tokens.END);
		return new FunctionBody(parameters, varargs, body);
	}

	private String name() throws IOException {
		return consume(Tokens.NAMESPACE).text;
	}

	private DoBlock doBlock() throws IOException {
		consume(Tokens.DO);
		Block body = block();
		consume(Tokens.END);
		return new DoBlock(body);
	}

	private RepeatBlock repeatBlock() throws IOException {
		consume(Tokens.REPEAT);
		Block body = block();
		consume(Tokens.UNTIL);
		Expression condition = expression();
		return new RepeatBlock(body, condition);
	}

	private WhileLoop whileLoop() throws IOException {
		consume(Tokens.WHILE);
		Expression condition = expression();
		consume(Tokens.DO);
		Block body = block();
		consume(Tokens.END);
		return new WhileLoop(condition, body);
	}

	private Goto gotoStatement() throws IOException {
		consume(Tokens.GOTO);
		String name = name();
		while (match(';'));
		return new Goto(name);
	}

	private Statement forLoop() throws IOException {
		consume(Tokens.FOR);
		String name = name();
		switch (current().type) {
			case '=':
				return forNum(name);
			case ',':
			case Tokens.IN:
				return forList(name);
			default:
				throw new IllegalStateException("Illegal token @ " + current());
		}
	}

	private ForLoop forNum(String name) throws IOException {
		consume('=');
		Expression lowerBound = expression();
		consume(',');
		Expression upperBound = expression();
		Expression step;
		if (match(',')) {
			step = expression();
		} else {
			step = new Literal(Literal.INTEGER, 1);
		}
		consume(Tokens.DO);
		Block body = block();
		consume(Tokens.END);
		return new ForLoop(name, lowerBound, upperBound, step, body);
	}

	private ForList forList(String name) throws IOException {
		List<Name> names = new ArrayList<>();
		names.add(new Name(name, FLAG_LOCAL));
		while (match(',')) {
			names.add(new Name(name(), FLAG_LOCAL));
		}
		consume(Tokens.IN);
		Expression expression = expressionList();
		consume(Tokens.DO);
		Block body = block();
		consume(Tokens.END);
		return new ForList(names, expression, body);
	}

	private IfBlock ifBlock() throws IOException {
		return ifBlock0(Tokens.IF);
	}

	private IfBlock ifBlock0(int type) throws IOException {
		consume(type);
		Expression condition = expression();
		consume(Tokens.THEN);
		Block body = block();
		Statement elsePart;
		if (is(Tokens.ELSEIF)) {
			elsePart = ifBlock0(Tokens.ELSEIF);
		} else {
			elsePart = match(Tokens.ELSE)
					? block()
					: null;
			consume(Tokens.END);
		}
		return new IfBlock(condition, body, elsePart);
	}

	public TableConstructor tableConstructor() throws IOException {
		consume('{');
		List<TableField> fields = new ArrayList<>();
		do {
			if (is('}')) {
				break;
			}
			fields.add(tableField());
		} while (match(',') || match(';'));
		consume('}');
		return new TableConstructor(fields);
	}

	public TableField tableField() throws IOException {
		Token current = current();
		if (current.type == '[') {
			consume();
			Expression key = expression();
			consume(']');
			consume('=');
			Expression value = expression();
			return new TableField(key, value);
		} else if (current.type == Tokens.NAMESPACE) {
			// @CLEANUP Jezza - 20 Jan 2018: Ok, so this is a lovely little hack.
			// Because we don't treat = as a binary operator, eg, not an expression,
			// we can attempt to parse an expression, it can return a namespace literal.
			// Then if the next token is an equal character, we can guess that it was
			// just a key.
			// All of this because I couldn't be bothered to implement 2 token lookahead.
			Expression expression = expression();
			if (match('=')) {
				if (!(expression instanceof Name)) {
					throw new IllegalStateException("Assertion invalid.");
				}
				Expression value = expression();
				return new TableField(expression, value);
			}
			return new TableField(null, expression);
		}
		return new TableField(null, expression());
	}

	public ExpressionList expressionList() throws IOException {
		Expression first = expression();
		if (!match(','))
			return new ExpressionList(first);
		List<Expression> expressions = new ArrayList<>();
		expressions.add(first);
		do {
			expressions.add(expression());
		} while (match(','));
		return new ExpressionList(expressions);
	}

	private ExpressionList expressionList(int expected) throws IOException {
		ExpressionList rhs = expressionList();

		ListIterator<Expression> it = rhs.list.listIterator();
		while (it.hasNext()) {
			Expression exp = it.next();
			if (expected == 0) {
				// remove all of the side-effect free expressions.
				if (exp instanceof Name || exp instanceof Literal) {
					it.remove();
				} else if (exp instanceof FunctionCall) {
					((FunctionCall) exp).expectedResults = 0;
				}
			} else	if (exp instanceof FunctionCall && !it.hasNext()) {
				((FunctionCall) exp).expectedResults = expected;
				expected = 0;
				break;
			} else {
				if (exp instanceof FunctionCall) {
					((FunctionCall) exp).expectedResults = 1;
				}
				expected--;
			}
		}
		while (expected > 0) {
			expected--;
			it.add(new Literal(Literal.NIL, null));
		}

//		} if (expected < 0) {
//			 a = b, c
//		}
		return rhs;
	}

	private static int binaryOp(int type) {
		switch (type) {
			case '+':
				return BinaryOp.OP_ADD;
			case '-':
				return BinaryOp.OP_SUB;
			case '*':
				return BinaryOp.OP_MUL;
			case '/':
				return BinaryOp.OP_DIV;
			case '%':
				return BinaryOp.OP_MOD;
			case '^':
				return BinaryOp.OP_POW;
			case Tokens.CONCAT:
				return BinaryOp.OP_CONCAT;
			case Tokens.NE:
				return BinaryOp.OP_NE;
			case Tokens.EQ:
				return BinaryOp.OP_EQ;
			case '<':
				return BinaryOp.OP_LT;
			case Tokens.LE:
				return BinaryOp.OP_LE;
			case '>':
				return BinaryOp.OP_GT;
			case Tokens.GE:
				return BinaryOp.OP_GE;
			case Tokens.AND:
				return BinaryOp.OP_AND;
			case Tokens.OR:
				return BinaryOp.OP_OR;
			default:
				return -1;
		}
	}

	private static int unaryOp(int type) {
		if (type == '-') {
			return UnaryOp.OP_MINUS;
		} else if (type == Tokens.NOT) {
			return UnaryOp.OP_NOT;
		} else if (type == '#') {
			return UnaryOp.OP_LEN;
		} else {
			return -1;
		}
	}

	private static final int[] PRIORITY = new int[]
			{
					6, 6, 6, 6,                 // + -
					7, 7, 7, 7, 7, 7,           // * / %
					10, 9, 5, 4,                // power and concat (right associative)
					3, 3, 3, 3,                 // equality and inequality
					3, 3, 3, 3, 3, 3, 3, 3,     // order
					2, 2, 1, 1                  // logical (and/or)
			};

	/**
	 * Priority for unary operators.
	 */
	private static final int UNARY_PRIORITY = 8;

	private Expression expression() throws IOException {
		return expression(0);
	}

	private Expression expression(int limit) throws IOException {
		Expression left;
		int unaryOp = unaryOp(current().type);
		if (unaryOp != -1) {
			consume();
			left = new UnaryOp(unaryOp, expression(UNARY_PRIORITY));
		} else {
			left = simpleExpression();
		}
		int op = binaryOp(current().type);
		while (op >= 0 && PRIORITY[op] > limit) {
			consume();
			Expression right = expression(PRIORITY[op + 1]);
			left = new BinaryOp(op, left, right);
			op = binaryOp(current().type);
		}
		return left;
	}

	private Expression simpleExpression() throws IOException {
		Token current = current();
		switch (current.type) {
			case Tokens.INTEGER:
				consume();
				try {
					Integer value = Integer.valueOf(current.text);
					return new Literal(Literal.INTEGER, value);
				} catch (NumberFormatException e) {
					throw new IllegalStateException("Invalid number: " + current, e);
				}
			case Tokens.DOUBLE:
				consume();
				try {
					Double value = Double.valueOf(current.text);
					return new Literal(Literal.DOUBLE, value);
				} catch (NumberFormatException e) {
					throw new IllegalStateException("Invalid number: " + current, e);
				}
			case Tokens.STRING:
				consume();
				return new Literal(Literal.STRING, current.text);
			case Tokens.NIL:
				consume();
				return new Literal(Literal.NIL, null);
			case Tokens.TRUE:
				consume();
				return new Literal(Literal.TRUE, Boolean.TRUE);
			case Tokens.FALSE:
				consume();
				return new Literal(Literal.FALSE, Boolean.FALSE);
			case Tokens.DOTS:
				consume();
				return new Varargs();
			case '{':
				return tableConstructor();
			case Tokens.FUNCTION:
				consume();
				return functionBody();
			default:
				return primaryExpression();
		}
	}

	private Expression prefix() throws IOException {
		Token token = consume();
		int type = token.type;
		if (type == '(') {
			Expression expression = expression();
			consume(')');
			return expression;
		} else if (type == Tokens.NAMESPACE) {
			return new Name(token.text, FLAG_UNCHECKED);
		} else {
			throw new IllegalStateException("Syntax: " + token);
		}
	}

	public Expression functionArguments() throws IOException {
		int type = current().type;
		if (type == '(') {
			consume();
			if (!match(')')) {
				Expression value = expressionList();
				consume(')');
				return value;
			}
			return new ExpressionList(new ArrayList<>());
		} else if (type == '{') {
			return tableConstructor();
		} else if (type == Tokens.STRING) {
			return new Literal(Tokens.STRING, consume().text);
		} else {
			throw new IllegalStateException("Syntax: " + current());
		}
	}

	private Expression primaryExpression() throws IOException {
		// NAME | '(' expr ')'
		Expression left = prefix();
		while (true) {
			switch (current().type) {
				case '.': {  // field
					consume();
					Literal right = new Literal(Literal.STRING, name());
					left = new BinaryOp(BinaryOp.OP_INDEXED, left, right);
					break;
				}
				case '[': { // '[' exp ']'
					consume();
					Expression right = expression();
					left = new BinaryOp(BinaryOp.OP_INDEXED, left, right);
					consume(']');
					// chain field
					break;
				}
				case ':':  { // ':' NAME functionArgs
					consume();
					left = new FunctionCall(left, name(), functionArguments());
					break;
				}
				case '(':
				case Tokens.STRING:
				case '{':
					left = new FunctionCall(left, null, functionArguments());
					break;
				default:
					return left;
			}
		}
	}

	private Assignment expressionStatement() throws IOException {
		Expression primary = primaryExpression();
		// Assignment or function call
		if (primary instanceof FunctionCall) {
			((FunctionCall) primary).expectedResults = 0;
			return new Assignment(null, primary);
		}
		primary.set(FLAG_ASSIGNMENT);
		ExpressionList lhs;
		if (match(',')) {
			List<Expression> expressions = new ArrayList<>();
			expressions.add(primary);
			do {
				Expression expression = primaryExpression();
				if (expression instanceof FunctionCall) {
					throw new IllegalStateException("Syntax error (Function call not allowed on left-hand side of assign): " + expression);
				}
				expression.set(FLAG_ASSIGNMENT);
				expressions.add(expression);
			} while (match(','));
			lhs = new ExpressionList(expressions);
		} else {
			lhs = new ExpressionList(primary);
		}
		consume('=');
		ExpressionList rhs = expressionList(lhs.size());
		return new Assignment(lhs, rhs);
	}

	private static boolean blockFollowing(int type) {
		return type == Tokens.ELSE
				|| type == Tokens.ELSEIF
				|| type == Tokens.END
				|| type == Tokens.EOS
				|| type == Tokens.UNTIL;
	}
}
