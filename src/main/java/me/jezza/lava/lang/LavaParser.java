package me.jezza.lava.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.jezza.lava.lang.ast.Tree.Block;
import me.jezza.lava.lang.ast.Tree.Break;
import me.jezza.lava.lang.ast.Tree.DoBlock;
import me.jezza.lava.lang.ast.Tree.Expression;
import me.jezza.lava.lang.ast.Tree.ExpressionList;
import me.jezza.lava.lang.ast.Tree.FunctionBody;
import me.jezza.lava.lang.ast.Tree.FunctionName;
import me.jezza.lava.lang.ast.Tree.FunctionStatement;
import me.jezza.lava.lang.ast.Tree.Goto;
import me.jezza.lava.lang.ast.Tree.IfBlock;
import me.jezza.lava.lang.ast.Tree.Label;
import me.jezza.lava.lang.ast.Tree.ParameterList;
import me.jezza.lava.lang.ast.Tree.RepeatBlock;
import me.jezza.lava.lang.ast.Tree.ReturnStatement;
import me.jezza.lava.lang.ast.Tree.Statement;
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
		LavaLexer lexer = new LavaLexer("if true then function x.x(ee) end elseif true then function y.y(ff) end else function z.z(gg) end end");
		LavaParser parser = new LavaParser(lexer);

		Block chunk = parser.chunk();
	}

	public Block chunk() throws IOException {
		Block block = block();
		if (!match(Tokens.EOS))
			System.out.println("File should have ended: " + current());
		return block;
	}

	public Block block() throws IOException {
		match(';');
		List<Statement> statements = new ArrayList<>();
		while (!blockFollowing(current().type)) {
			statements.add(statement());
			match(';');
			// TODO: 08/06/2017 Should we push this into the statement method?
			if (match(Tokens.END)) {
				match(';');
				break;
			} else if (match(Tokens.BREAK)) {
				statements.add(new Break());
				match(';');
				break;
			} else if (match(Tokens.RETURN)) {
				statements.add(new ReturnStatement(expressionList()));
				match(';');
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
			default:
//				expStat();
//				return null;
				throw new IllegalStateException("NYI");
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
		return new WhileLoop(condition, body);
	}

	private Goto gotoStatement() throws IOException {
		consume(Tokens.GOTO);
		return new Goto(name());
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
		} else if (match(Tokens.ELSE)) {
			elsePart = block();
		} else {
			elsePart = null;
		}
		return new IfBlock(condition, body, elsePart);
	}

	public ExpressionList expressionList() throws IOException {
		List<Expression> expressions = new ArrayList<>();
		do {
			expressions.add(expression());
		} while (match(','));
		return new ExpressionList(wrap(expressions));
	}

	private Expression expression() throws IOException {
		consume(Tokens.TRUE);
		return new TrueExpression();
//		throw new IllegalStateException("NYI");
	}

	public static final class TrueExpression extends Expression {

		@Override
		public void visit(Visitor visitor) {
			visitor.visitExpression(this);
		}

		@Override
		public String toString() {
			return "true";
		}
	}

//	private void primaryExp(ExpDesc value) throws IOException {
//		prefixExp(value);
//		while (true) {
//			switch (current().type) {
//				case '.': {
//					field(value);
//					break;
//				}
//				case '[': {
//					ExpDesc key = new ExpDesc();
//					index(key);
//					value.kind = ExpDesc.V_INDEXED;
//					value.next = key;
//					break;
//				}
//				case ':': {
//					ExpDesc key = new ExpDesc();
//					consume();
//					name(key);
//					// self shit
//					funcArgs(value);
//					break;
//				}
//				case '(':
//				case '{':
//				case Tokens.STRING: {
//					funcArgs(value);
//					break;
//				}
//				default:
//					return;
//			}
//		}
//	}
//
//	private void prefixExp(ExpDesc value) throws IOException {
//		int type = current().type;
//		if (type == '(') {
//			consume();
//			exp(value);
//			consume(')');
//		} else if (type == Tokens.NAMESPACE) {
//			name(value);
//		} else {
//			throw new IllegalStateException("Syntax");
//		}
//	}
//
//	private void exp(ExpDesc value) throws IOException {
//		exp0(value, 0);
//	}
//
//	private static final int OPR_ADD = 0;
//	private static final int OPR_SUB = 1;
//	private static final int OPR_MUL = 2;
//	private static final int OPR_DIV = 3;
//	private static final int OPR_MOD = 4;
//	private static final int OPR_POW = 5;
//	private static final int OPR_CONCAT = 6;
//	private static final int OPR_NE = 7;
//	private static final int OPR_EQ = 8;
//	private static final int OPR_LT = 9;
//	private static final int OPR_LE = 10;
//	private static final int OPR_GT = 11;
//	private static final int OPR_GE = 12;
//	private static final int OPR_AND = 13;
//	private static final int OPR_OR = 14;
//
//	private static int binaryIndex(int type) {
//		switch (type) {
//			case '+':
//				return OPR_ADD;
//			case '-':
//				return OPR_SUB;
//			case '*':
//				return OPR_MUL;
//			case '/':
//				return OPR_DIV;
//			case '%':
//				return OPR_MOD;
//			case '^':
//				return OPR_POW;
//			case Tokens.CONCAT:
//				return OPR_CONCAT;
//			case Tokens.NE:
//				return OPR_NE;
//			case Tokens.EQ:
//				return OPR_EQ;
//			case '<':
//				return OPR_LT;
//			case Tokens.LE:
//				return OPR_LE;
//			case '>':
//				return OPR_GT;
//			case Tokens.GE:
//				return OPR_GE;
//			case Tokens.AND:
//				return OPR_AND;
//			case Tokens.OR:
//				return OPR_OR;
//			default:
//				return -1;
//		}
//	}
//
//	private static boolean isUnary(int type) {
//		return type == Tokens.NOT
//				|| type == '-'
//				|| type == '#';
//	}
//
//	private static final int[] PRIORITY = new int[]
//			{
//					6, 6, 6, 6,                 // + -
//					7, 7, 7, 7, 7, 7,         // * / %
//					10, 9, 5, 4,                // power and concat (right associative)
//					3, 3, 3, 3,                 // equality and inequality
//					3, 3, 3, 3, 3, 3, 3, 3, // order
//					2, 2, 1, 1                  // logical (and/or)
//			};
//
//	/**
//	 * Priority for unary operators.
//	 */
//	private static final int UNARY_PRIORITY = 8;
//
//	private int exp0(ExpDesc value, int limit) throws IOException {
//		if (isUnary(current().type)) {
//			consume();
//			exp0(value, UNARY_PRIORITY);
//		} else {
//			simpleExp(value);
//		}
//		int index = binaryIndex(current().type);
//		while (index >= 0 && PRIORITY[index] > limit) {
//			ExpDesc sub = new ExpDesc();
//			consume();
//			index = exp0(sub, PRIORITY[index + 1]);
//		}
//		return index;
//	}
//
//	private void simpleExp(ExpDesc value) throws IOException {
//		Token current = current();
//		switch (current.type) {
//			case Tokens.INTEGER:
//				value.kind = ExpDesc.V_CONSTANT_INTEGER;
//				value.aux = Double.parseDouble(current.text);
//				break;
//			case Tokens.FLOAT:
//				value.kind = ExpDesc.V_CONSTANT_FLOAT;
//				value.aux = Double.parseDouble(current.text);
//				break;
//			case Tokens.STRING:
//				name(value);
//				return;
//			case Tokens.NIL:
//				value.kind = ExpDesc.V_NIL;
//				break;
//			case Tokens.TRUE:
//				value.kind = ExpDesc.V_TRUE;
//				break;
//			case Tokens.FALSE:
//				value.kind = ExpDesc.V_FALSE;
//				break;
//			case Tokens.DOTS:
//				value.kind = ExpDesc.V_VARARG;
//				break;
//			case '{':
//				constructor(value);
//				return;
//			case Tokens.FUNCTION:
//				consume();
//				body(value, false);
//				return;
//			default:
//				primaryExp(value);
//				return;
//		}
//		consume();
//	}
//
//	private void constructor(ExpDesc table) throws IOException {
//		table.kind = ExpDesc.V_RELOCABLE;
//		ExpDesc field = new ExpDesc();
//		consume('{');
//		if (!match('}')) {
//			do {
//				Token current = consume();
//				if (current.type == Tokens.NAMESPACE) {
//					if (lookahead('=')) {
//						recField(field);
//					} else {
//						exp(field);
//					}
//				} else if (current.type == '[') {
//					recField(field);
//				} else {
//					exp(field);
//				}
//
//			} while (match(','));
//			consume('}');
//		}
//	}
//
//	private void recField(ExpDesc field) throws IOException {
//		ExpDesc key = new ExpDesc();
//		ExpDesc value = new ExpDesc();
//		if (lookahead(Tokens.NAMESPACE)) {
//			name(key);
//		} else {
//			index(key);
//		}
//		consume('=');
//		exp(value);
//	}
//
//	private void index(ExpDesc value) throws IOException {
//		consume('[');
//		exp(value);
//		consume(']');
//	}
//
//	public static final class ExpDesc {
//		static final int V_VOID = 0;             // no value
//		static final int V_NIL = 1;
//		static final int V_TRUE = 2;
//		static final int V_FALSE = 3;
//		static final int V_CONSTANT = 4;        // info = index into 'k'
//		static final int V_CONSTANT_INTEGER = 5; // nval = numerical value
//		static final int V_CONSTANT_FLOAT = 6; // nval = numerical value
//		static final int V_LOCAL = 7;            // info = local register
//		static final int V_UPVAL = 8;            // info = index into 'upvalues'
//		static final int V_GLOBAL = 9;           // info = index of table;
//		// aux = index of global name in 'k'
//		static final int V_INDEXED = 10;          // info = table register
//		// aux = index register (or 'k')
//		static final int V_JMP = 11;             // info = instruction pc
//		static final int V_RELOCABLE = 12;       // info = instruction pc
//		static final int V_NONRELOC = 13;        // info = result register
//		static final int V_CALL = 14;            // info = instruction pc
//		static final int V_VARARG = 15;          // info = instruction pc
//
//		private static final ExpDesc SELF = new ExpDesc(V_LOCAL, "self");
//
//		private int kind;
//		private Object aux;
//		private ExpDesc next;
//
//		public ExpDesc() {
//		}
//
//		private ExpDesc(int kind, Object aux) {
//			this.kind = kind;
//			this.aux = aux;
//		}
//	}

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
