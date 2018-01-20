package me.jezza.lava.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.jezza.lava.lang.ast.ParseTree.Assignment;
import me.jezza.lava.lang.ast.ParseTree.BinaryOp;
import me.jezza.lava.lang.ast.ParseTree.Block;
import me.jezza.lava.lang.ast.ParseTree.Break;
import me.jezza.lava.lang.ast.ParseTree.DoBlock;
import me.jezza.lava.lang.ast.ParseTree.Expression;
import me.jezza.lava.lang.ast.ParseTree.ExpressionList;
import me.jezza.lava.lang.ast.ParseTree.ForList;
import me.jezza.lava.lang.ast.ParseTree.ForLoop;
import me.jezza.lava.lang.ast.ParseTree.FunctionBody;
import me.jezza.lava.lang.ast.ParseTree.FunctionCall;
import me.jezza.lava.lang.ast.ParseTree.Goto;
import me.jezza.lava.lang.ast.ParseTree.IfBlock;
import me.jezza.lava.lang.ast.ParseTree.Label;
import me.jezza.lava.lang.ast.ParseTree.Literal;
import me.jezza.lava.lang.ast.ParseTree.LocalFunction;
import me.jezza.lava.lang.ast.ParseTree.LocalStatement;
import me.jezza.lava.lang.ast.ParseTree.ParameterList;
import me.jezza.lava.lang.ast.ParseTree.RepeatBlock;
import me.jezza.lava.lang.ast.ParseTree.ReturnStatement;
import me.jezza.lava.lang.ast.ParseTree.Statement;
import me.jezza.lava.lang.ast.ParseTree.TableConstructor;
import me.jezza.lava.lang.ast.ParseTree.TableField;
import me.jezza.lava.lang.ast.ParseTree.UnaryOp;
import me.jezza.lava.lang.ast.ParseTree.Varargs;
import me.jezza.lava.lang.ast.ParseTree.WhileLoop;
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
		return new Block(statements);
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
						? new ExpressionList(Collections.emptyList())
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
			return new LocalFunction(name(), functionBody());
		} else {
			// local statement
			List<String> names = new ArrayList<>();
			do {
				names.add(name());
			} while (match(','));
			ExpressionList expression = match('=')
					? expressionList()
					: new ExpressionList(Collections.emptyList());
			return new LocalStatement(names, expression);
		}
	}

	public Statement functionStatement() throws IOException {
		consume(Tokens.FUNCTION);
		Expression left = new Literal(Literal.NAMESPACE, name());
		while (match('.')) {
			left = new BinaryOp(BinaryOp.OPR_INDEXED, left, new Literal(Literal.NAMESPACE, name()));
		}
		boolean self = match(':');
		if (self) {
			left = new BinaryOp(BinaryOp.OPR_INDEXED, left, new Literal(Literal.NAMESPACE, name()));
		}
		ExpressionList prefix = new ExpressionList(List.of(left));
		FunctionBody body = functionBody();
		if (self) {
			body.parameterList.nameList.add(0, "self");
		}
		return new Assignment(prefix, new ExpressionList(List.of(body)));
	}

	private FunctionBody functionBody() throws IOException {
		consume('(');
		ParameterList parameters;
		if (!match(')')) {
			List<String> names = new ArrayList<>();
			boolean varargs;
			do {
				// @TODO Jezza - 08 Jun 2017: Probably not the best lookahead...
				if (varargs = match(Tokens.DOTS)) {
					break;
				}
				names.add(name());
			} while (match(','));
			parameters = new ParameterList(names, varargs);
			consume(')');
		} else {
			parameters = new ParameterList(new ArrayList<>(), false);
		}
		Block body = block();
		consume(Tokens.END);
		return new FunctionBody(parameters, body);
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
		List<String> names = new ArrayList<>();
		names.add(name);
		while (match(','))
			names.add(name());
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
		List<TableField> fields = new ArrayList<>();;
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
			if (current().type == '=') {
				if (!(expression instanceof Literal) || ((Literal) expression).type != Literal.NAMESPACE) {
					throw new IllegalStateException("Assertion invalid. During table construction only ");
				}
				consume('=');
				Expression value = expression();
				return new TableField(expression, value);
			}
			return new TableField(null, expression);
		} else {
			return new TableField(null, expression());
		}
	}

	public ExpressionList expressionList() throws IOException {
		Expression first = expression();
		if (!match(','))
			return new ExpressionList(List.of(first));
		List<Expression> expressions = new ArrayList<>();
		expressions.add(first);
		do {
			expressions.add(expression());
		} while (match(','));
		return new ExpressionList(expressions);
	}

	private static int binaryOp(int type) {
		switch (type) {
			case '+':
				return BinaryOp.OPR_ADD;
			case '-':
				return BinaryOp.OPR_SUB;
			case '*':
				return BinaryOp.OPR_MUL;
			case '/':
				return BinaryOp.OPR_DIV;
			case '%':
				return BinaryOp.OPR_MOD;
			case '^':
				return BinaryOp.OPR_POW;
			case Tokens.CONCAT:
				return BinaryOp.OPR_CONCAT;
			case Tokens.NE:
				return BinaryOp.OPR_NE;
			case Tokens.EQ:
				return BinaryOp.OPR_EQ;
			case '<':
				return BinaryOp.OPR_LT;
			case Tokens.LE:
				return BinaryOp.OPR_LE;
			case '>':
				return BinaryOp.OPR_GT;
			case Tokens.GE:
				return BinaryOp.OPR_GE;
			case Tokens.AND:
				return BinaryOp.OPR_AND;
			case Tokens.OR:
				return BinaryOp.OPR_OR;
			default:
				return -1;
		}
	}

	private static int unaryOp(int type) {
		if (type == '-') {
			return UnaryOp.OPR_MINUS;
		} else if (type == Tokens.NOT) {
			return UnaryOp.OPR_NOT;
		} else if (type == '#') {
			return UnaryOp.OPR_LEN;
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
			return new Literal(Literal.NAMESPACE, token.text);
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
			return new ExpressionList(Collections.emptyList());
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
					Literal right = new Literal(Literal.NAMESPACE, name());
					left = new BinaryOp(BinaryOp.OPR_INDEXED, left, right);
					break;
				}
				case '[': { // '[' exp ']'
					consume();
					Expression right = expression();
					left = new BinaryOp(BinaryOp.OPR_INDEXED, left, right);
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

	private Statement expressionStatement() throws IOException {
		Expression primary = primaryExpression();
		// Assignment or function call
		if (primary instanceof FunctionCall) {
			return new Assignment(null, new ExpressionList(List.of(primary)));
		} else {
			ExpressionList leftSide;
			if (match(',')) {
				List<Expression> expressions = new ArrayList<>();
				expressions.add(primary);
				do {
					expressions.add(primaryExpression());
				} while (match(','));
				leftSide = new ExpressionList(expressions);
			} else if (primary instanceof ExpressionList){
				leftSide = ((ExpressionList) primary);
			} else {
				leftSide = new ExpressionList(List.of(primary));
			}
			consume('=');
			return new Assignment(leftSide, expressionList());
		}
	}

	private static boolean blockFollowing(int type) {
		return type == Tokens.ELSE
				|| type == Tokens.ELSEIF
				|| type == Tokens.END
				|| type == Tokens.EOS
				|| type == Tokens.UNTIL;
	}
}
