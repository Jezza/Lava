package me.jezza.lava.lang;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.jezza.lava.lang.ast.Tree.Assignment;
import me.jezza.lava.lang.ast.Tree.BinaryOp;
import me.jezza.lava.lang.ast.Tree.Block;
import me.jezza.lava.lang.ast.Tree.Break;
import me.jezza.lava.lang.ast.Tree.DoBlock;
import me.jezza.lava.lang.ast.Tree.Expression;
import me.jezza.lava.lang.ast.Tree.ExpressionList;
import me.jezza.lava.lang.ast.Tree.ForList;
import me.jezza.lava.lang.ast.Tree.ForLoop;
import me.jezza.lava.lang.ast.Tree.FunctionBody;
import me.jezza.lava.lang.ast.Tree.FunctionCall;
import me.jezza.lava.lang.ast.Tree.FunctionName;
import me.jezza.lava.lang.ast.Tree.FunctionStatement;
import me.jezza.lava.lang.ast.Tree.Goto;
import me.jezza.lava.lang.ast.Tree.IfBlock;
import me.jezza.lava.lang.ast.Tree.IndexField;
import me.jezza.lava.lang.ast.Tree.Label;
import me.jezza.lava.lang.ast.Tree.ListField;
import me.jezza.lava.lang.ast.Tree.Literal;
import me.jezza.lava.lang.ast.Tree.LocalFunction;
import me.jezza.lava.lang.ast.Tree.LocalStatement;
import me.jezza.lava.lang.ast.Tree.ParameterList;
import me.jezza.lava.lang.ast.Tree.RepeatBlock;
import me.jezza.lava.lang.ast.Tree.ReturnStatement;
import me.jezza.lava.lang.ast.Tree.Statement;
import me.jezza.lava.lang.ast.Tree.TableConstructor;
import me.jezza.lava.lang.ast.Tree.TableField;
import me.jezza.lava.lang.ast.Tree.UnaryOp;
import me.jezza.lava.lang.ast.Tree.Varargs;
import me.jezza.lava.lang.ast.Tree.WhileLoop;
import me.jezza.lava.lang.base.AbstractParser;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public final class LavaParser extends AbstractParser {
	public LavaParser(Lexer lexer) {
		super(lexer);
	}

	public static void main(String[] args) throws IOException {
//		LavaLexer lexer = new LavaLexer("x, (1)[test] = 4");
		LavaLexer lexer = new LavaLexer(new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\TempShit\\binarytrees.lua"));
		LavaParser parser = new LavaParser(lexer);

		long start = System.nanoTime();
		Block chunk = parser.chunk();
		long end = System.nanoTime();
		System.out.println(end - start);
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
			match(';');
			statement = statement();
			match(';');
			if (statement == null)
				break;
			statements.add(statement);
			if (statement instanceof Break || statement instanceof ReturnStatement) {
				break;
			}
		}
		return new Block(wrap(statements));
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

			case Tokens.END:
				consume();
				return null;
			case Tokens.BREAK:
				consume();
				return new Break();
			case Tokens.RETURN:
				consume();
				if (blockFollowing(current().type) || match(';')) {
					return new ReturnStatement(new ExpressionList(Collections.emptyList()));
				}
				return new ReturnStatement(expressionList());

			case ':':
				return label();
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
			Expression expression = match('=') ? expressionList() : new ExpressionList(Collections.emptyList());
			return new LocalStatement(wrap(names), expression);
		}
	}

	public FunctionStatement functionStatement() throws IOException {
		consume(Tokens.FUNCTION);
		FunctionName name = functionName();
		FunctionBody body = functionBody();
		return new FunctionStatement(name, body);
	}

	private FunctionName functionName() throws IOException {
		String first = name();
		List<String> names = new ArrayList<>();
		while (match('.'))
			names.add(name());
		String self = match(':') ? name() : null;
		return new FunctionName(first, wrap(names), self);
	}

	private FunctionBody functionBody() throws IOException {
		consume('(');
		final ParameterList parameterList;
		if (!match(')')) {
			parameterList = parameterList();
			consume(')');
		} else {
			parameterList = null;
		}
		Block body = block();
		consume(Tokens.END);
		return new FunctionBody(parameterList, body);
	}

	private ParameterList parameterList() throws IOException {
		List<String> nameList = nameList();
		boolean varargs = match(Tokens.DOTS);
		return new ParameterList(nameList, varargs);
	}

	private List<String> nameList() throws IOException {
		// SIDE EFFECT
		List<String> names = new ArrayList<>();
		do {
			// TODO: 08/06/2017 Just temp
			if (lookahead(Tokens.DOTS))
				break;
			names.add(name());
		} while (match(','));
		return wrap(names);
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
		return new Goto(name());
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
		final Expression step;
		if (match(',')) {
			step = expression();
		} else {
			step = new Literal(Tokens.INTEGER, 1);
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
		final Statement elsePart;
		if (lookahead(Tokens.ELSEIF)) {
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
		final List<TableField> fields;
		if (!match('}')) {
			fields = new ArrayList<>();
			do {
				fields.add(field());
			} while (match(',') || match(';'));
			consume('}');
		} else {
			fields = Collections.emptyList();
		}
		return new TableConstructor(fields);
	}

	public TableField field() throws IOException {
		Token current = current();
		if (current.type == '[') {
			consume();
			Expression key = expression();
			consume(']');
			consume('=');
			Expression value = expression();
			return new IndexField(key, value);
		} else if (current.type == Tokens.NAMESPACE) {
			if (peek(1).type == '=') {
				Expression key = new Literal(Tokens.NAMESPACE, consume().text);
				consume('=');
				Expression value = expression();
				return new IndexField(key, value);
			} else {
				Expression key = new Literal(Tokens.NAMESPACE, consume().text);
				return new ListField(key);
			}
		} else {
			Expression value = expression();
			return new ListField(value);
		}
	}

	public Expression expressionList() throws IOException {
		Expression first = expression();
		if (!match(','))
			return first;
		List<Expression> expressions = new ArrayList<>();
		expressions.add(first);
		do {
			expressions.add(expression());
		} while (match(','));
		return new ExpressionList(wrap(expressions));
	}

	public static final int OPR_ADD = 0;
	public static final int OPR_SUB = 1;
	public static final int OPR_MUL = 2;
	public static final int OPR_DIV = 3;
	public static final int OPR_MOD = 4;
	public static final int OPR_POW = 5;
	public static final int OPR_CONCAT = 6;
	public static final int OPR_NE = 7;
	public static final int OPR_EQ = 8;
	public static final int OPR_LT = 9;
	public static final int OPR_LE = 10;
	public static final int OPR_GT = 11;
	public static final int OPR_GE = 12;
	public static final int OPR_AND = 13;
	public static final int OPR_OR = 14;

	public static final int OPR_MINUS = 0;
	public static final int OPR_NOT = 1;
	public static final int OPR_LEN = 2;

	private static int binaryOp(int type) {
		switch (type) {
			case '+':
				return OPR_ADD;
			case '-':
				return OPR_SUB;
			case '*':
				return OPR_MUL;
			case '/':
				return OPR_DIV;
			case '%':
				return OPR_MOD;
			case '^':
				return OPR_POW;
			case Tokens.CONCAT:
				return OPR_CONCAT;
			case Tokens.NE:
				return OPR_NE;
			case Tokens.EQ:
				return OPR_EQ;
			case '<':
				return OPR_LT;
			case Tokens.LE:
				return OPR_LE;
			case '>':
				return OPR_GT;
			case Tokens.GE:
				return OPR_GE;
			case Tokens.AND:
				return OPR_AND;
			case Tokens.OR:
				return OPR_OR;
			default:
				return -1;
		}
	}

	private static int unaryOp(int type) {
		if (type == '-') {
			return OPR_MINUS;
		} else if (type == Tokens.NOT) {
			return OPR_NOT;
		} else if (type == '#') {
			return OPR_LEN;
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
				return new Literal(Tokens.INTEGER, Integer.valueOf(current.text));
			case Tokens.FLOAT:
				consume();
				return new Literal(Tokens.FLOAT, Double.valueOf(current.text));
			case Tokens.STRING:
				consume();
				return new Literal(Tokens.STRING, current.text);
			case Tokens.NIL:
				consume();
				return new Literal(Tokens.NIL, null);
			case Tokens.TRUE:
				consume();
				return new Literal(Tokens.TRUE, Boolean.TRUE);
			case Tokens.FALSE:
				consume();
				return new Literal(Tokens.FALSE, Boolean.FALSE);
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

	private Expression variable() throws IOException {
		Token token = consume();
		int type = token.type;
		if (type == '(') {
			Expression expression = expression();
			consume(')');
			return expression;
		} else if (type == Tokens.NAMESPACE) {
			return new Literal(Tokens.NAMESPACE, token.text);
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
		List<Expression> expressions = new ArrayList<>();
		// NAME | '(' expr ')'
		Expression prefix = variable();
		expressions.add(prefix);
		while (true) {
			switch (current().type) {
				case '.': {  // field
					consume();
					expressions.add(new Literal(Tokens.NAMESPACE, name()));
					// chain field
					break;
				}
				case '[': { // '[' exp ']'
					consume();
					expressions.add(expression());
					consume(']');
					// chain field
					break;
				}
				case ':':  { // ':' NAME functionArgs
					consume();
					FunctionCall functionCall = new FunctionCall(expressions, name(), functionArguments());
					expressions = new ArrayList<>();
					expressions.add(functionCall);
					break;
				}
				case '(':
				case Tokens.STRING:
				case '{':
					FunctionCall functionCall = new FunctionCall(expressions, null, functionArguments());
					expressions = new ArrayList<>();
					expressions.add(functionCall);
					break;
				default:
					return expressions.size() == 1 ? expressions.get(0) : new ExpressionList(expressions);
			}
		}
	}

	private Statement expressionStatement() throws IOException {
		Expression primary = primaryExpression();
		// Assignment or function call
		if (primary instanceof FunctionCall) {
			return new Assignment(null, primary);
		} else {
			if (match(',')) {
				List<Expression> expressions = new ArrayList<>();
				expressions.add(primary);
				do {
					expressions.add(primaryExpression());
				} while (match(','));
				primary = new ExpressionList(expressions);
			}
			consume('=');
			return new Assignment(primary, expressionList());
		}
	}

	private static boolean blockFollowing(int type) {
		return type == Tokens.ELSE
				|| type == Tokens.ELSEIF
				|| type == Tokens.END
				|| type == Tokens.EOS
				|| type == Tokens.UNTIL;
	}

	public static <T> List<T> wrap(List<T> in) {
		if (in == null || in.isEmpty()) {
			return Collections.emptyList();
		} else if (in.size() == 1) {
			return Collections.singletonList(in.get(0));
		} else {
			if (in instanceof ArrayList)
				((ArrayList<?>) in).trimToSize();
			return Collections.unmodifiableList(in);
		}
	}
}
