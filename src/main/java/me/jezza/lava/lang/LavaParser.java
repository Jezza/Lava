package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.Block.FLAG_CONTROL_FLOW_BARRIER;
import static me.jezza.lava.lang.ParseTree.Block.FLAG_CONTROL_FLOW_EXIT;
import static me.jezza.lava.lang.ParseTree.Block.FLAG_NEW_CONTEXT;
import static me.jezza.lava.lang.ParseTree.FLAG_ASSIGNMENT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;

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
import me.jezza.lava.lang.base.AbstractParser;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public final class LavaParser extends AbstractParser {
	public LavaParser(Lexer lexer) {
		super(lexer);
	}

	public FunctionBody chunk() throws IOException {
		Block body = block();
		if (!match(Tokens.EOS)) {
			System.out.println("File should have ended: " + current());
		}
		List<Statement> statements = body.statements;
		if (statements.isEmpty() || !(statements.get(statements.size() - 1) instanceof ReturnStatement)) {
			statements.add(new ReturnStatement(new ExpressionList()));
		}
		return new FunctionBody(new ArrayList<>(), true, body);
	}

	public Block block() throws IOException {
		List<Statement> statements = new ArrayList<>();
		while (!blockFollowing(current().type)) {
			if (statement(statements)) {
				break;
			}
		}
		return new Block(statements);
	}

	private static boolean blockFollowing(int type) {
		return type == Tokens.ELSE
				|| type == Tokens.ELSEIF
				|| type == Tokens.END
				|| type == Tokens.EOS
				|| type == Tokens.UNTIL;
	}

	public boolean statement(List<Statement> statements) throws IOException {
		Statement statement;
		switch (current().type) {
			case Tokens.DO:
				statement = doBlock();
				break;
			case Tokens.LOCAL:
				local(statements);
				return false;
			case Tokens.FUNCTION:
				statement = functionStatement();
				break;
			case Tokens.GOTO:
				statement = gotoStatement();
				break;
			case Tokens.REPEAT:
				statement = repeatBlock();
				break;
			case Tokens.IF:
				statement = ifBlock();
				break;
			case Tokens.WHILE:
				statement = whileLoop();
				break;
			case Tokens.FOR:
				statement = forLoop();
				break;
			case Tokens.BREAK:
				consume();
				while (match(';')) {
					;
				}
				statements.add(new Break());
				return true;
			case Tokens.RETURN:
				consume();
				// @CLEANUP Jezza - 20 Jan 2018: This is a bit messy...
				ExpressionList expressions = blockFollowing(current().type) || match(';')
						? new ExpressionList()
						: expressionList();
				match(';');
				statements.add(new ReturnStatement(expressions));
				return true;
			case ':':
				statement = label();
				break;
			case ';':
				consume();
				return false;
			default:
				statement = expressionStatement();
				break;
		}
		statements.add(statement);
		return false;
	}

	public void local(List<Statement> statements) throws IOException {
		consume(Tokens.LOCAL);
		if (match(Tokens.FUNCTION)) {
			String functionName = name();
			// local function f() ... end -> local f; f = function() ... end
			Name name = new Name(functionName, FLAG_LOCAL | FLAG_ASSIGNMENT);
			Assignment declaration = new Assignment(name, Literal.NIL_LITERAL);
			FunctionBody functionBody = functionBody();
			Assignment assignment = new Assignment(name, functionBody);
			statements.add(declaration);
			statements.add(assignment);
			return;
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
				values.add(Literal.NIL_LITERAL);
			} while (--size > 0);
			rhs = new ExpressionList(values);
		}
		statements.add(new Assignment(lhs, rhs));
	}

	public Statement functionStatement() throws IOException {
		consume(Tokens.FUNCTION);
		Expression left = new Name(name(), 0);
		while (match('.')) {
			left = new BinaryOp(BinaryOp.OP_INDEXED, left, new Literal(Literal.STRING, name()));
		}
		boolean self = match(':');
		if (self) {
			left = new BinaryOp(BinaryOp.OP_INDEXED, left, new Literal(Literal.STRING, name()));
		}
		left.set(FLAG_ASSIGNMENT, true);
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
		body.set(FLAG_NEW_CONTEXT, true);
		body.set(FLAG_CONTROL_FLOW_BARRIER, true);
		consume(Tokens.END);
		List<Statement> statements = body.statements;
		if (statements.isEmpty()
				|| !(statements.get(statements.size() - 1) instanceof ReturnStatement)) {
			statements.add(new ReturnStatement(new ExpressionList()));
		}
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
		body.set(FLAG_CONTROL_FLOW_EXIT, true);
		consume(Tokens.UNTIL);
		Expression condition = expression();
		return new RepeatBlock(body, condition);
	}

	private Statement whileLoop() throws IOException {
		consume(Tokens.WHILE);
		Expression condition = expression();
		consume(Tokens.DO);
		Block body = block();
		body.set(FLAG_CONTROL_FLOW_EXIT, true);
		consume(Tokens.END);
		// Lowering: While (cond) (body) -> If (cond) Repeat (body) Until (not(cond))
		RepeatBlock repeatBlock = new RepeatBlock(body, new UnaryOp(UnaryOp.OP_NOT, condition));
		Block loop = new Block(repeatBlock);
		return new IfBlock(condition, loop, null);
	}

	private Goto gotoStatement() throws IOException {
		consume(Tokens.GOTO);
		String name = name();
		return new Goto(name);
	}

	public Label label() throws IOException {
		consume(':');
		consume(':');
		String name = name();
		consume(':');
		consume(':');
		return new Label(name);
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
				throw new IllegalStateException("Unexpected token: " + current());
		}
	}

	private static final String VAR_NAME = "(var)";
	private static final String LIMIT_NAME = "(limit)";
	private static final String STEP_NAME = "(step)";
	private static final String LABEL_NAME = "(for_loop)";

	private Statement forNum(String name) throws IOException {
		consume('=');
		Expression lowerBound = expression();
		consume(',');
		Expression upperBound = expression();
		Expression step = match(',')
				? expression()
				: new Literal(Literal.INTEGER, 1);
		consume(Tokens.DO);
		Block block = block();
		consume(Tokens.END);
		/*
		for v = e1, e2, e3 do
			{block}
		end
		
		do
			local var, limit, step = tonumber(e1), tonumber(e2), tonumber(e3)
			if not (var and limit and step) then
				error("for loop parameters must evaluate to numbers")
			end
			var = var - step
			while true do
				var = var + step
				if (step >= 0 and var > limit) or (step < 0 and var < limit) then
					break
				end
				local v = var
				{block}
			end
		end
		 */

		List<Statement> statements = new ArrayList<>();

		// local (var), (limit), (step) = tonumber(e1), tonumber(e2), tonumber(e3)
		{
			ExpressionList left = new ExpressionList();
			ExpressionList right = new ExpressionList();

			// local (var) = tonumber(e1)
			Name var = new Name(VAR_NAME, FLAG_LOCAL | FLAG_ASSIGNMENT);
			left.list.add(var);
			right.list.add(new UnaryOp(UnaryOp.OP_TO_NUMBER, lowerBound));

			// local (limit) = tonumber(e2)
			Name limit = new Name(LIMIT_NAME, FLAG_LOCAL | FLAG_ASSIGNMENT);
			left.list.add(limit);
			right.list.add(new UnaryOp(UnaryOp.OP_TO_NUMBER, upperBound));

			// local (step) = tonumber(e3)
			Name increment = new Name(STEP_NAME, FLAG_LOCAL | FLAG_ASSIGNMENT);
			left.list.add(increment);
			right.list.add(new UnaryOp(UnaryOp.OP_TO_NUMBER, step));

			statements.add(new Assignment(left, right));
		}

		// if not ((var) and (limit) and (step)) then error("blah") end
		{
			// error("blah")
			List<Statement> thenBlock = new ArrayList<>();
			UnaryOp error = new UnaryOp(UnaryOp.OP_ERROR, new Literal(Literal.STRING, "for loop parameters must evaluate to numbers"));
			thenBlock.add(new Assignment(null, error));

			Name var = new Name(VAR_NAME, FLAG_LOCAL);
			Name limit = new Name(LIMIT_NAME, FLAG_LOCAL);
			Name increment = new Name(STEP_NAME, FLAG_LOCAL);

			// not (((var) and (limit)) and (step))
			Expression condition = new UnaryOp(UnaryOp.OP_NOT,
					new BinaryOp(BinaryOp.OP_AND, new BinaryOp(BinaryOp.OP_AND, var, limit), increment));

			statements.add(new IfBlock(condition, new Block(thenBlock), null));
		}

		// var = var - step
		{
			BinaryOp op = new BinaryOp(BinaryOp.OP_SUB, new Name(VAR_NAME, FLAG_LOCAL), new Name(STEP_NAME, FLAG_LOCAL));
			statements.add(new Assignment(new Name(VAR_NAME, FLAG_LOCAL | FLAG_ASSIGNMENT), op));
		}

		// while true do ... end
		{
			List<Statement> inner = new ArrayList<>();

			// var = var + step
			{
				BinaryOp op = new BinaryOp(BinaryOp.OP_ADD, new Name(VAR_NAME, FLAG_LOCAL), new Name(STEP_NAME, FLAG_LOCAL));
				inner.add(new Assignment(new Name(VAR_NAME, FLAG_LOCAL | FLAG_ASSIGNMENT), op));
			}

			// if (step >= 0 and var > limit) or (step < 0 and var < limit) then
			//   break
			// end
			{

				BinaryOp baseLeft;
				{
					// step >= 0
					BinaryOp left = new BinaryOp(BinaryOp.OP_GE, new Name(STEP_NAME, FLAG_LOCAL), new Literal(Literal.INTEGER, 0));
					// var > limit
					BinaryOp right = new BinaryOp(BinaryOp.OP_GT, new Name(VAR_NAME, FLAG_LOCAL), new Name(LIMIT_NAME, FLAG_LOCAL));

					// (step >= 0 and var > limit)
					baseLeft = new BinaryOp(BinaryOp.OP_AND, left, right);
				}

				BinaryOp baseRight;
				{
					// step < 0
					BinaryOp left = new BinaryOp(BinaryOp.OP_LT, new Name(STEP_NAME, FLAG_LOCAL), new Literal(Literal.INTEGER, 0));
					// var < limit
					BinaryOp right = new BinaryOp(BinaryOp.OP_LT, new Name(VAR_NAME, FLAG_LOCAL), new Name(LIMIT_NAME, FLAG_LOCAL));

					// (step < 0 and var < limit)
					baseRight = new BinaryOp(BinaryOp.OP_AND, left, right);
				}

				// (step >= 0 and var > limit) or (step < 0 and var < limit)
				BinaryOp condition = new BinaryOp(BinaryOp.OP_OR, baseLeft, baseRight);

				// break
				Block body = new Block(new Goto(LABEL_NAME));

				inner.add(new IfBlock(condition, body, null));
			}

			// local v = (var)
			{
				Name var = new Name(VAR_NAME, FLAG_LOCAL);
				inner.add(new Assignment(new Name(name, FLAG_LOCAL | FLAG_ASSIGNMENT), var));
			}

			// {block}
			{
				inner.addAll(block.statements);
			}

			RepeatBlock repeatBlock = new RepeatBlock(new Block(inner), Literal.FALSE_LITERAL);
			repeatBlock.set(FLAG_CONTROL_FLOW_EXIT, true);
			statements.add(repeatBlock);
		}

		{
			statements.add(new Label(LABEL_NAME));
		}

		return new DoBlock(new Block(statements));
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
		int index = 1;
		do {
			if (is('}')) {
				break;
			}
			TableField field = tableField();
			if (field.key == null) {
				field.key = new Literal(Literal.INTEGER, index++);
			}
			fields.add(field);
		} while (match(',') || match(';'));
		consume('}');
		return new TableConstructor(fields);
	}

	private TableField tableField() throws IOException {
		Expression key;
		Expression value;
		Token current = current();
		if (current.type == '[') {
			consume();
			key = expression();
			consume(']');
			consume('=');
			value = expression();
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
				key = new Literal(Literal.STRING, ((Name) expression).value);
				value = expression();
			} else {
				key = null;
				value = expression;
			}
		} else {
			key = null;
			value = expression();
		}
		return new TableField(key, value);
	}

	public ExpressionList expressionList() throws IOException {
		Expression first = expression();
		if (!match(',')) {
			return new ExpressionList(first);
		}
		List<Expression> expressions = new ArrayList<>();
		expressions.add(first);
		do {
			expressions.add(expression());
		} while (match(','));
		return new ExpressionList(expressions);
	}

	private ExpressionList expressionList(int expected) throws IOException {
		// @CLEANUP Jezza - 30 Mar 2018: Combine this call, and the iterator stuff.
		ExpressionList rhs = expressionList();

		ListIterator<Expression> it = rhs.list.listIterator();
		while (it.hasNext()) {
			Expression exp = it.next();
			if (expected == 0) {
				// remove all of the side-effect free expressions.
				if (exp instanceof Name || exp instanceof Literal || exp instanceof Varargs) {
					it.remove();
				} else if (exp instanceof FunctionCall) {
					((FunctionCall) exp).expectedResults = 0;
				}
			} else if (exp instanceof FunctionCall && !it.hasNext()) {
				((FunctionCall) exp).expectedResults = expected;
				expected = 0;
				break;
			} else if (exp instanceof Varargs && !it.hasNext()) {
				((Varargs) exp).expectedResults = expected;
				expected = 0;
				break;
			} else {
				if (exp instanceof FunctionCall) {
					((FunctionCall) exp).expectedResults = 1;
				} else if (exp instanceof Varargs) {
					((Varargs) exp).expectedResults = 1;
				}
				expected--;
			}
		}
		while (expected > 0) {
			expected--;
			it.add(Literal.NIL_LITERAL);
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

	private static final int[] PRIORITY = {
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
		int op;
		while ((op = binaryOp(current().type) << 1) >= 0 && PRIORITY[op] > limit) {
			consume();
			Expression right = expression(PRIORITY[op + 1]);
			left = new BinaryOp(op >> 1, left, right);
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
				return Literal.NIL_LITERAL;
			case Tokens.TRUE:
				consume();
				return Literal.TRUE_LITERAL;
			case Tokens.FALSE:
				consume();
				return Literal.FALSE_LITERAL;
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
			return new Name(token.text, 0);
		} else {
			throw new IllegalStateException("Syntax: " + token);
		}
	}

	public ExpressionList functionArguments() throws IOException {
		int type = current().type;
		if (type == '(') {
			consume();
			if (!match(')')) {
				ExpressionList value = expressionList();
				consume(')');
				return value;
			}
			return new ExpressionList();
		} else if (type == '{') {
			return new ExpressionList(tableConstructor());
		} else if (type == Tokens.STRING) {
			return new ExpressionList(new Literal(Tokens.STRING, consume().text));
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
				case ':': { // ':' NAME functionArgs
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
		primary.set(FLAG_ASSIGNMENT, true);
		ExpressionList lhs;
		if (match(',')) {
			List<Expression> expressions = new ArrayList<>();
			expressions.add(primary);
			do {
				Expression expression = primaryExpression();
				if (expression instanceof FunctionCall) {
					throw new IllegalStateException("Syntax error (Function call not allowed on left-hand side of assign): " + expression);
				}
				expression.set(FLAG_ASSIGNMENT, true);
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
}
