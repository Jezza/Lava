package me.jezza.lava.lang;

import java.util.ArrayList;
import java.util.List;

import me.jezza.lava.Strings;
import me.jezza.lava.lang.interfaces.Visitor;
import me.jezza.lava.lang.interfaces.Visitor.EVisitor;
import me.jezza.lava.lang.interfaces.Visitor.PVisitor;
import me.jezza.lava.lang.interfaces.Visitor.RVisitor;

/**
 * @author Jezza
 */
public abstract class ParseTree {
	private static final int TYPE_BLOCK = 0;
	private static final int TYPE_EXPRESSION_LIST = 1;
	private static final int TYPE_DO_BLOCK = 2;
	private static final int TYPE_WHILE_LOOP = 3;
	private static final int TYPE_REPEAT_BLOCK = 4;
	private static final int TYPE_IF_BLOCK = 5;
	private static final int TYPE_FOR_LOOP = 6;
	private static final int TYPE_FOR_LIST = 7;
	private static final int TYPE_FUNCTION_BODY = 8;
	private static final int TYPE_LOCAL_STATEMENT = 9;
	private static final int TYPE_ASSIGNMENT = 10;
	private static final int TYPE_UNARY_OP = 11;
	private static final int TYPE_BINARY_OP = 12;
	private static final int TYPE_FUNCTION_CALL = 13;
	private static final int TYPE_TABLE_CONSTRUCTOR = 14;
	private static final int TYPE_TABLE_FIELD = 15;
	private static final int TYPE_RETURN_STATEMENT = 16;
	private static final int TYPE_LITERAL = 17;
	private static final int TYPE_BREAK = 18;
	private static final int TYPE_GOTO = 19;
	private static final int TYPE_LABEL = 20;
	private static final int TYPE_VARARGS = 21;

	public static final int FLAG_ASSIGNMENT = 0x1;

	private final int type;
	private int flags;

	ParseTree(int type) {
		this.type = type;
	}

	public boolean is(int flags) {
		return (this.flags & flags) == flags;
	}

	public ParseTree set(int flags) {
		this.flags |= flags;
		return this;
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
			case TYPE_EXPRESSION_LIST:
				return visitor.visitExpressionList((ExpressionList) this, userObject);
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
			case TYPE_LOCAL_STATEMENT:
				return visitor.visitLocalStatement((LocalStatement) this, userObject);
			case TYPE_ASSIGNMENT:
				return visitor.visitAssignment((Assignment) this, userObject);
			case TYPE_UNARY_OP:
				return visitor.visitUnaryOp((UnaryOp) this, userObject);
			case TYPE_BINARY_OP:
				return visitor.visitBinaryOp((BinaryOp) this, userObject);
			case TYPE_FUNCTION_CALL:
				return visitor.visitFunctionCall((FunctionCall) this, userObject);
			case TYPE_TABLE_CONSTRUCTOR:
				return visitor.visitTableConstructor((TableConstructor) this, userObject);
			case TYPE_TABLE_FIELD:
				return visitor.visitTableField((TableField) this, userObject);
			case TYPE_RETURN_STATEMENT:
				return visitor.visitReturnStatement((ReturnStatement) this, userObject);
			case TYPE_LITERAL:
				return visitor.visitLiteral((Literal) this, userObject);
			case TYPE_LABEL:
				return visitor.visitLabel((Label) this, userObject);
			case TYPE_BREAK:
				return visitor.visitBreak((Break) this, userObject);
			case TYPE_GOTO:
				return visitor.visitGoto((Goto) this, userObject);
			case TYPE_VARARGS:
				return visitor.visitVarargs((Varargs) this, userObject);
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

		@Override
		public String toString() {
			return Strings.format("Block{statements={}}",
					statements);
		}
	}

	public static final class ExpressionList extends Expression {
		public List<Expression> list;

		public ExpressionList(Expression value) {
			super(TYPE_EXPRESSION_LIST);
			list = new ArrayList<>();
			list.add(value);
		}

		public ExpressionList(List<Expression> list) {
			super(TYPE_EXPRESSION_LIST);
			this.list = list;
		}

		public int size() {
			return list.size();
		}

		@Override
		public String toString() {
			return Strings.format("ExpressionList{list={}}",
					list);
		}
	}

	public static final class DoBlock extends Statement {
		public Block body;

		public DoBlock(Block body) {
			super(TYPE_DO_BLOCK);
			this.body = body;
		}

		@Override
		public String toString() {
			return Strings.format("DoBlock{body={}}",
					body);
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

		@Override
		public String toString() {
			return Strings.format("WhileLoop{condition={}, body={}}",
					condition,
					body);
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

		@Override
		public String toString() {
			return Strings.format("RepeatBlock{body={}, condition={}}",
					body,
					condition);
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

		@Override
		public String toString() {
			return Strings.format("IfBlock{condition={}, thenPart={}, elsePart={}}",
					condition,
					thenPart,
					elsePart);
		}
	}

	public static final class ForLoop extends Statement {
		public String name;
		public Expression lowerBound;
		public Expression upperBound;
		public Expression step;

		public Block body;

		public ForLoop(String name, Expression lowerBound, Expression upperBound, Expression step, Block body) {
			super(TYPE_FOR_LOOP);
			this.name = name;
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
			this.step = step;
			this.body = body;
		}

		@Override
		public String toString() {
			return Strings.format("ForLoop{name=\"{}\", lowerBound={}, upperBound={}, step={}, body={}}",
					name,
					lowerBound,
					upperBound,
					step,
					body);
		}
	}

	public static final class ForList extends Statement {
		public List<String> nameList;
		public Expression expList;
		public Block body;

		public ForList(List<String> nameList, Expression expList, Block body) {
			super(TYPE_FOR_LIST);
			this.nameList = nameList;
			this.expList = expList;
			this.body = body;
		}

		@Override
		public String toString() {
			return Strings.format("ForList{nameList={}, expList={}, body={}}",
					nameList,
					expList,
					body);
		}
	}

	public static final class FunctionBody extends Expression {
		public List<String> parameters;
		public boolean varargs;
		public Block body;

		public FunctionBody(List<String> parameters, boolean varargs, Block body) {
			super(TYPE_FUNCTION_BODY);
			this.parameters = parameters;
			this.varargs = varargs;
			this.body = body;
		}

		@Override
		public String toString() {
			return Strings.format("FunctionBody{parameters={}, varargs={}, body={}}",
					parameters,
					varargs,
					body);
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

		@Override
		public String toString() {
			return Strings.format("LocalStatement{lhs={}, rhs={}}",
					lhs,
					rhs);
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

		@Override
		public String toString() {
			return Strings.format("Assignment{lhs={}, rhs={}}",
					lhs,
					rhs);
		}
	}

	public static final class UnaryOp extends Expression {
		public static final int OP_MINUS = 0;
		public static final int OP_NOT = 1;
		public static final int OP_LEN = 2;

		public int op;
		public Expression arg;

		public UnaryOp(int op, Expression arg) {
			super(TYPE_UNARY_OP);
			this.op = op;
			this.arg = arg;
		}

		@Override
		public String toString() {
			return Strings.format("UnaryOp{op={}, arg={}}",
					op,
					arg);
		}
	}

	public static final class BinaryOp extends Expression {
		public static final int OP_ADD = 0;
		public static final int OP_SUB = 2;
		public static final int OP_MUL = 4;
		public static final int OP_DIV = 6;
		public static final int OP_MOD = 8;

		public static final int OP_POW = 10;
		public static final int OP_CONCAT = 12;

		public static final int OP_NE = 14;
		public static final int OP_EQ = 16;

		public static final int OP_LT = 18;
		public static final int OP_LE = 20;
		public static final int OP_GT = 22;
		public static final int OP_GE = 24;

		public static final int OP_AND = 26;
		public static final int OP_OR = 28;
		public static final int OP_INDEXED = 30;

		public int op;
		public Expression left;
		public Expression right;

		public BinaryOp(int op, Expression left, Expression right) {
			super(TYPE_BINARY_OP);
			this.op = op;
			this.left = left;
			this.right = right;
		}

		@Override
		public String toString() {
			return Strings.format("BinaryOp{op={}, left={}, right={}}",
					op,
					left,
					right);
		}
	}

	public static final class FunctionCall extends Expression {
		public Expression target;
		public String name;
		public Expression args;
		public int argCount;

		public FunctionCall(Expression target, String name, Expression args) {
			super(TYPE_FUNCTION_CALL);
			this.target = target;
			this.name = name;
			this.args = args;
			argCount = args instanceof ExpressionList
					? ((ExpressionList) args).size()
					: 1;
		}

		@Override
		public String toString() {
			return Strings.format("FunctionCall{target={}, name=\"{}\", args={}}",
					target,
					name,
					args);
		}
	}

	public static final class TableConstructor extends Expression {
		public List<TableField> fields;

		public TableConstructor(List<TableField> fields) {
			super(TYPE_TABLE_CONSTRUCTOR);
			this.fields = fields;
		}

		@Override
		public String toString() {
			return Strings.format("TableConstructor{fields={}}",
					fields);
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

		@Override
		public String toString() {
			return Strings.format("TableField{key={}, value={}}",
					key,
					value);
		}
	}

	public static final class ReturnStatement extends Statement {
		public ExpressionList exprs;

		public ReturnStatement(ExpressionList exprs) {
			super(TYPE_RETURN_STATEMENT);
			this.exprs = exprs;
		}

		@Override
		public String toString() {
			return Strings.format("ReturnStatement{exprs={}}",
					exprs);
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

		@Override
		public String toString() {
			return Strings.format("Literal{type={}, value={}}",
					type,
					value);
		}
	}

	public static final class Break extends Statement {
		public Break() {
			super(TYPE_BREAK);
		}

		@Override
		public String toString() {
			return "Break{}";
		}
	}

	public static final class Goto extends Statement {
		public String label;

		public Goto(String label) {
			super(TYPE_GOTO);
			this.label = label;
		}

		@Override
		public String toString() {
			return Strings.format("Goto{label=\"{}\"}",
					label);
		}
	}

	public static final class Label extends Statement {
		public String name;

		public Label(String name) {
			super(TYPE_LABEL);
			this.name = name;
		}

		@Override
		public String toString() {
			return Strings.format("Label{name=\"{}\"}",
					name);
		}
	}

	public static final class Varargs extends Expression {
		public Varargs() {
			super(TYPE_VARARGS);
		}
	}
}
