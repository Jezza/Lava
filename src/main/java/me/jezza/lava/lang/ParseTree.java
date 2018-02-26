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
	private static final int TYPE_REPEAT_BLOCK = 4;
	private static final int TYPE_IF_BLOCK = 5;
	private static final int TYPE_FOR_LOOP = 6;
	private static final int TYPE_FOR_LIST = 7;
	private static final int TYPE_FUNCTION_BODY = 8;
	private static final int TYPE_ASSIGNMENT = 9;
	private static final int TYPE_UNARY_OP = 10;
	private static final int TYPE_BINARY_OP = 11;
	private static final int TYPE_FUNCTION_CALL = 12;
	private static final int TYPE_TABLE_CONSTRUCTOR = 13;
	private static final int TYPE_TABLE_FIELD = 14;
	private static final int TYPE_RETURN_STATEMENT = 15;
	private static final int TYPE_LITERAL = 16;
	private static final int TYPE_NAME = 17;
	private static final int TYPE_BREAK = 18;
	private static final int TYPE_GOTO = 19;
	private static final int TYPE_LABEL = 20;
	private static final int TYPE_VARARGS = 21;

	public static final int FLAG_ASSIGNMENT = 0x1;

	private final int type;
	private int flags;
	
//	public Block block;

	ParseTree(int type) {
		this.type = type;
	}

	public final boolean is(int flags) {
		return (this.flags & flags) == flags;
	}

	public final ParseTree set(int flags) {
		this.flags |= flags;
		return this;
	}
	
	public final ParseTree unset(int flags) {
		this.flags &= ~flags;
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
			case TYPE_NAME:
				return visitor.visitName((Name) this, userObject);
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
		public static final int VARARGS = -1;
		
		public String name;
		public Block parent;

		public List<Statement> statements;

		public List<String> names;

		public int parameterCount;

		public Block(String name, Statement statement) {
			this(name, new ArrayList<>(1));
			statements.add(statement);
		}

		public Block(String name, List<Statement> statements) {
			super(TYPE_BLOCK);
			this.name = name;
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
			this(new ArrayList<>(1));
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
		public List<Name> nameList;
		public Expression expressions;
		public Block body;

		public ForList(List<Name> nameList, Expression expressions, Block body) {
			super(TYPE_FOR_LIST);
			this.nameList = nameList;
			this.expressions = expressions;
			this.body = body;
		}

		@Override
		public String toString() {
			return Strings.format("ForList{nameList={}, expressions={}, body={}}",
					nameList,
					expressions,
					body);
		}
	}

	public static final class FunctionBody extends Expression {
		public List<Name> parameters;
		public boolean varargs;
		public Block body;

		public FunctionBody(List<Name> parameters, boolean varargs, Block body) {
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

	public static final class Assignment extends Statement {
		public ExpressionList lhs;
		public ExpressionList rhs;

		public Assignment(Expression lhs, Expression rhs) {
			this(lhs != null ? new ExpressionList(lhs) : null, new ExpressionList(rhs));
		}

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
		public ExpressionList args;

		public int expectedResults;

		public FunctionCall(Expression target, String name, ExpressionList args) {
			super(TYPE_FUNCTION_CALL);
			this.target = target;
			this.name = name;
			this.args = args;
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
		public ExpressionList expressions;

		public ReturnStatement(ExpressionList expressions) {
			super(TYPE_RETURN_STATEMENT);
			this.expressions = expressions;
		}

		@Override
		public String toString() {
			return Strings.format("ReturnStatement{expressions={}}",
					expressions);
		}
	}
	
	public static final class Name extends Expression {
		public static final int FLAG_LOCAL = 0x2;
		public static final int FLAG_UPVAL = 0x4;
		public static final int FLAG_GLOBAL = 0x8;
		public static final int FLAG_UNCHECKED = 0x10;

		public String value;
		public int index;

		public Name(String value) {
			this(value, FLAG_UNCHECKED);
		}

		public Name(String value, int flags) {
			super(TYPE_NAME);
			this.value = value;
			if (flags != 0) {
				set(flags);
			}
			index = -1;
		}

		@Override
		public String toString() {
			return Strings.format("Name{value=\"{}\"}",
					value);
		}
	}

	public static final class Literal extends Expression {
		public static final int INTEGER = 1;
		public static final int DOUBLE = 2;
		public static final int STRING = 3;
		public static final int TRUE = 4;
		public static final int FALSE = 5;
		public static final int NIL = 6;

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
