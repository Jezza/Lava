package me.jezza.lava.lang.ast;

import java.util.List;

import me.jezza.lava.lang.interfaces.Visitor;
import me.jezza.lava.lang.interfaces.Visitor.EVisitor;
import me.jezza.lava.lang.interfaces.Visitor.PVisitor;
import me.jezza.lava.lang.interfaces.Visitor.RVisitor;

/**
 * @author Jezza
 */
public abstract class ParseTree {
	private static final int TYPE_BLOCK = 0;
	private static final int TYPE_LABEL = 1;
	private static final int TYPE_BREAK = 2;
	private static final int TYPE_GOTO = 3;
	private static final int TYPE_DO_BLOCK = 4;
	private static final int TYPE_WHILE_LOOP = 5;
	private static final int TYPE_REPEAT_BLOCK = 6;
	private static final int TYPE_IF_BLOCK = 7;
	private static final int TYPE_FOR_LOOP = 8;
	private static final int TYPE_FOR_LIST = 9;
	private static final int TYPE_FUNCTION_BODY = 11;
	private static final int TYPE_LOCAL_FUNCTION = 13;
	private static final int TYPE_LOCAL_STATEMENT = 14;
	private static final int TYPE_RETURN_STATEMENT = 15;
	private static final int TYPE_PARAMETER_LIST = 16;
	private static final int TYPE_EXPRESSION_LIST = 17;
	private static final int TYPE_UNARY_OP = 18;
	private static final int TYPE_BINARY_OP = 19;
	private static final int TYPE_LITERAL = 20;
	private static final int TYPE_VARARGS = 21;
	private static final int TYPE_FUNCTION_CALL = 22;
	private static final int TYPE_VARIABLE = 23;
	private static final int TYPE_ASSIGNMENT = 24;
	private static final int TYPE_TABLE_CONSTRUCTOR = 25;
	private static final int TYPE_TABLE_FIELD = 26;

	private final int type;

	ParseTree(int type) {
		this.type = type;
	}

	public final void visit(EVisitor visitor) {
		visit(visitor, null);
	}

	public final <R> R visit(RVisitor<R> visitor) {
		return visit(visitor, null);
	}

	public final <P> void visit(PVisitor<P> visitor, P userObject) {
		visit((Visitor<P, ?>) visitor, userObject);
	}

	public final <R> R visit(Visitor<?, R> visitor) {
		return visit(visitor, null);
	}

	@SuppressWarnings("unchecked")
	public final <P, R> R visit(Visitor<P, R> visitor, P userObject) {
		switch (type) {
			case TYPE_BLOCK:
				return visitor.visitBlock((Block) this, userObject);
			case TYPE_LABEL:
				return visitor.visitLabel((Label) this, userObject);
			case TYPE_BREAK:
				return visitor.visitBreak((Break) this, userObject);
			case TYPE_GOTO:
				return visitor.visitGoto((Goto) this, userObject);
			case TYPE_DO_BLOCK:
				return visitor.visitDoBlock((DoBlock) this, userObject);
			case TYPE_WHILE_LOOP:
				return visitor.visitWhileLoop((WhileLoop) this, userObject);
			case TYPE_REPEAT_BLOCK:
				return visitor.visitRepeatBlock((RepeatBlock) this, userObject);
			case TYPE_IF_BLOCK:
				return visitor.visitIfBlock((IfBlock) this, userObject);
			case TYPE_FOR_LOOP:
				return visitor.visitForLoop((ForLoop) this, userObject);
			case TYPE_FOR_LIST:
				return visitor.visitForList((ForList) this, userObject);
			case TYPE_FUNCTION_BODY:
				return visitor.visitFunctionBody((FunctionBody) this, userObject);
			case TYPE_LOCAL_FUNCTION:
				return visitor.visitLocalFunction((LocalFunction) this, userObject);
			case TYPE_LOCAL_STATEMENT:
				return visitor.visitLocalStatement((LocalStatement) this, userObject);
			case TYPE_RETURN_STATEMENT:
				return visitor.visitReturnStatement((ReturnStatement) this, userObject);
			case TYPE_PARAMETER_LIST:
				return visitor.visitParameterList((ParameterList) this, userObject);
			case TYPE_EXPRESSION_LIST:
				return visitor.visitExpressionList((ExpressionList) this, userObject);
			case TYPE_UNARY_OP:
				return visitor.visitUnaryOp((UnaryOp) this, userObject);
			case TYPE_BINARY_OP:
				return visitor.visitBinaryOp((BinaryOp) this, userObject);
			case TYPE_LITERAL:
				return visitor.visitLiteral((Literal) this, userObject);
			case TYPE_VARARGS:
				return visitor.visitVarargs((Varargs) this, userObject);
			case TYPE_FUNCTION_CALL:
				return visitor.visitFunctionCall((FunctionCall) this, userObject);
			case TYPE_VARIABLE:
				return visitor.visitVariable((Variable) this, userObject);
			case TYPE_ASSIGNMENT:
				return visitor.visitAssignment((Assignment) this, userObject);
			case TYPE_TABLE_CONSTRUCTOR:
				return visitor.visitTableConstructor((TableConstructor) this, userObject);
			case TYPE_TABLE_FIELD:
				return visitor.visitTableField((TableField) this, userObject);
			default:
				throw new IllegalStateException("Unknown subtype: " + type);
		}
	}

	public abstract static class Statement extends ParseTree {
		Statement(int type) {
			super(type);
		}
	}

	public abstract static class Expression extends ParseTree {
		Expression(int type) {
			super(type);
		}
	}

	public static class Block extends Statement {
		public List<Statement> statements;

		public Block(List<Statement> statements) {
			super(TYPE_BLOCK);
			this.statements = statements;
		}
	}

	public static final class Label extends Statement {
		public String name;

		public Label(String name) {
			super(TYPE_LABEL);
			this.name = name;
		}
	}

	public static final class Break extends Statement {
		public Break() {
			super(TYPE_BREAK);
		}
	}

	public static final class Goto extends Statement {
		public String label;

		public Goto(String label) {
			super(TYPE_GOTO);
			this.label = label;
		}
	}

	public static final class DoBlock extends Statement {
		public Block body;

		public DoBlock(Block body) {
			super(TYPE_DO_BLOCK);
			this.body = body;
		}
	}

	public static final class WhileLoop extends Statement {
		public Expression condition;
		public Block body;

		public WhileLoop(Expression condition, Block body) {
			super(TYPE_WHILE_LOOP);
			this.condition = condition;
			this.body = body;
		}
	}

	public static final class RepeatBlock extends Statement {
		public Block body;
		public Expression condition;

		public RepeatBlock(Block body, Expression condition) {
			super(TYPE_REPEAT_BLOCK);
			this.body = body;
			this.condition = condition;
		}
	}

	public static final class IfBlock extends Statement {
		public Expression condition;
		public Block thenPart;
		public Statement elsePart;

		public IfBlock(Expression condition, Block thenPart, Statement elsePart) {
			super(TYPE_IF_BLOCK);
			this.condition = condition;
			this.thenPart = thenPart;
			this.elsePart = elsePart;
		}
	}

	public static final class ForLoop extends Statement {
		public String name;
		public Expression lowerBound;
		public Expression upperBound;
		public Expression step;

		public Statement body;

		public ForLoop(String name, Expression lowerBound, Expression upperBound, Expression step, Statement body) {
			super(TYPE_FOR_LOOP);
			this.name = name;
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
			this.step = step;
			this.body = body;
		}
	}

	public static final class ForList extends Statement {
		public List<String> nameList;
		public Expression expList;
		public Statement body;

		public ForList(List<String> nameList, Expression expList, Statement body) {
			super(TYPE_FOR_LIST);
			this.nameList = nameList;
			this.expList = expList;
			this.body = body;
		}
	}

	public static final class FunctionBody extends Expression {
		public ParameterList parameterList;
		public Block body;

		public FunctionBody(ParameterList parameterList, Block body) {
			super(TYPE_FUNCTION_BODY);
			this.parameterList = parameterList;
			this.body = body;
		}
	}

	public static final class LocalFunction extends Statement {
		public String name;
		public FunctionBody body;

		public LocalFunction(String name, FunctionBody body) {
			super(TYPE_LOCAL_FUNCTION);
			this.name = name;
			this.body = body;
		}
	}

	public static final class LocalStatement extends Statement {
		public List<String> lhs;
		public ExpressionList rhs;

		public LocalStatement(List<String> lhs, ExpressionList rhs) {
			super(TYPE_LOCAL_STATEMENT);
			this.lhs = lhs;
			this.rhs = rhs;
		}
	}

	public static final class ReturnStatement extends Statement {
		public ExpressionList exprs;

		public ReturnStatement(ExpressionList exprs) {
			super(TYPE_RETURN_STATEMENT);
			this.exprs = exprs;
		}
	}

	public static final class ParameterList extends Statement {
		public List<String> nameList;
		public boolean varargs;

		public ParameterList(List<String> nameList, boolean varargs) {
			super(TYPE_PARAMETER_LIST);
			this.nameList = nameList;
			this.varargs = varargs;
		}
	}

	public static final class ExpressionList extends Expression {
		public List<Expression> list;

		public ExpressionList(List<Expression> list) {
			super(TYPE_EXPRESSION_LIST);
			this.list = list;
		}
	}

	public static final class UnaryOp extends Expression {
		public static final int OPR_MINUS = 0;
		public static final int OPR_NOT = 1;
		public static final int OPR_LEN = 2;

		public int op;
		public Expression arg;

		public UnaryOp(int op, Expression arg) {
			super(TYPE_UNARY_OP);
			this.op = op;
			this.arg = arg;
		}
	}

	public static final class BinaryOp extends Expression {
		public static final int OPR_ADD = 0;
		public static final int OPR_SUB = 2;
		public static final int OPR_MUL = 4;
		public static final int OPR_DIV = 8;
		public static final int OPR_MOD = 10;
		public static final int OPR_POW = 12;
		public static final int OPR_CONCAT = 14;
		public static final int OPR_NE = 16;
		public static final int OPR_EQ = 18;
		public static final int OPR_LT = 20;
		public static final int OPR_LE = 22;
		public static final int OPR_GT = 24;
		public static final int OPR_GE = 26;
		public static final int OPR_AND = 28;
		public static final int OPR_OR = 30;

		public int op;
		public Expression left;
		public Expression right;

		public BinaryOp(int op, Expression left, Expression right) {
			super(TYPE_BINARY_OP);
			this.op = op;
			this.left = left;
			this.right = right;
		}
	}

	public static final class Literal extends Expression {
		public static final int INTEGER = 1;
		public static final int DOUBLE = 2;
		public static final int STRING = 3;
		public static final int TRUE = 4;
		public static final int FALSE = 5;
		public static final int NAMESPACE = 6;
		public static final int NIL = 7;

		public int type;
		public Object value;

		public Literal(int type, Object value) {
			super(TYPE_LITERAL);
			this.type = type;
			this.value = value;
		}
	}

	public static final class Varargs extends Expression {
		public Varargs() {
			super(TYPE_VARARGS);
		}
	}

	public static final class FunctionCall extends Expression {
		public List<Expression> prefix;
		public String name;
		public Expression args;

		public FunctionCall(List<Expression> prefix, final String name, Expression args) {
			super(TYPE_FUNCTION_CALL);
			this.prefix = prefix;
			this.name = name;
			this.args = args;
		}
	}

	public static final class Variable extends Expression {
		public String name;

		public Variable(String name) {
			super(TYPE_VARIABLE);
			this.name = name;
		}
	}

	public static final class Assignment extends Statement {
		public ExpressionList lhs;
		public ExpressionList rhs;

		public Assignment(ExpressionList lhs, ExpressionList rhs) {
			super(TYPE_ASSIGNMENT);
			this.lhs = lhs;
			this.rhs = rhs;
		}
	}

	public static final class TableConstructor extends Expression {
		public List<TableField> fields;

		public TableConstructor(List<TableField> fields) {
			super(TYPE_TABLE_CONSTRUCTOR);
			this.fields = fields;
		}
	}

	public static final class TableField extends Expression {
		public Expression key;
		public Expression value;

		public TableField(Expression key, Expression value) {
			super(TYPE_TABLE_FIELD);
			this.key = key;
			this.value = value;
		}
	}
}
