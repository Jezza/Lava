package me.jezza.lava.lang.ast;

import java.util.List;

import me.jezza.lava.lang.Visitor;

/**
 * @author Jezza
 */
public abstract class Tree {

	public abstract void visit(Visitor visitor);

	public abstract static class Statement extends Tree {
	}

	public static class Block extends Statement {
		public final List<Statement> statements;

		public Block(List<Statement> statements) {
			this.statements = statements;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitBlock(this);
		}
	}

	public static final class Label extends Statement {
		public final String name;

		public Label(String name) {
			this.name = name;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitLabel(this);
		}
	}

	public static final class Break extends Statement {
		public Break() {
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitBreak(this);
		}
	}

	public static final class Goto extends Statement {
		public final String label;

		public Goto(String label) {
			this.label = label;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitGoto(this);
		}
	}

	public static final class DoBlock extends Statement {
		public final Block body;

		public DoBlock(Block body) {
			this.body = body;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitDoBlock(this);
		}
	}

	public static final class WhileLoop extends Statement {
		public final Expression condition;
		public final Block body;

		public WhileLoop(Expression condition, Block body) {
			this.condition = condition;
			this.body = body;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitWhileLoop(this);
		}
	}

	public static final class RepeatBlock extends Statement {
		public final Block body;
		public final Expression condition;

		public RepeatBlock(Block body, Expression condition) {
			this.body = body;
			this.condition = condition;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitRepeatBlock(this);
		}
	}

	public static final class IfBlock extends Statement {
		public final Expression condition;
		public final Block thenPart;
		public final Statement elsePart;

		public IfBlock(Expression condition, Block thenPart, Statement elsePart) {
			this.condition = condition;
			this.thenPart = thenPart;
			this.elsePart = elsePart;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitIfBlock(this);
		}
	}

	public static final class ForLoop extends Statement {
		public final String name;
		public final Expression lowerBound;
		public final Expression upperBound;
		public final Expression step;

		public final Statement body;

		public ForLoop(String name, Expression lowerBound, Expression upperBound, Expression step, Statement body) {
			this.name = name;
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
			this.step = step;
			this.body = body;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitForLoop(this);
		}
	}

	public static final class ForList extends Statement {
		public final List<String> nameList;
		public final Expression expList;
		public final Statement body;

		public ForList(List<String> nameList, Expression expList, Statement body) {
			this.nameList = nameList;
			this.expList = expList;
			this.body = body;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitForLoopIn(this);
		}
	}

	public static final class FunctionName extends Statement {
		public final String first;
		public final List<String> nested;
		public final String self;

		public FunctionName(String first, List<String> nested, String self) {
			this.first = first;
			this.nested = nested;
			this.self = self;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitFunctionName(this);
		}
	}

	public static final class FunctionBody extends Expression {
		public final ParameterList parameterList;
		public final Block body;

		public FunctionBody(ParameterList parameterList, Block body) {
			this.parameterList = parameterList;
			this.body = body;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitFunctionBody(this);
		}
	}

	public static final class FunctionStatement extends Statement {
		public final FunctionName name;
		public final FunctionBody body;

		public FunctionStatement(FunctionName name, FunctionBody body) {
			this.name = name;
			this.body = body;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitFunctionStatement(this);
		}
	}

	public static final class LocalFunction extends Statement {
		public final String name;
		public final FunctionBody body;

		public LocalFunction(String name, FunctionBody body) {
			this.name = name;
			this.body = body;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitLocalFunction(this);
		}
	}

	public static final class LocalStatement extends Statement {
		public final List<String> names;
		public final Expression expressions;

		public LocalStatement(List<String> names, Expression expressions) {
			this.names = names;
			this.expressions = expressions;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitLocalStatement(this);
		}
	}

	public static final class ReturnStatement extends Statement {
		public final Expression expressions;

		public ReturnStatement(Expression expressions) {
			this.expressions = expressions;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitReturnStatement(this);
		}
	}

	public static final class ParameterList extends Statement {
		public final List<String> nameList;
		public final boolean varargs;

		public ParameterList(List<String> nameList, boolean varargs) {
			this.nameList = nameList;
			this.varargs = varargs;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitParameterList(this);
		}
	}

	public static final class ExpressionList extends Expression {
		public final List<Expression> expList;

		public ExpressionList(List<Expression> expList) {
			this.expList = expList;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitExpressionList(this);
		}

	}

	public abstract static class Expression extends Tree {
		public Expression() {
		}
	}

	public static final class UnaryOp extends Expression {
		public final int op;
		public final Expression arg;

		public UnaryOp(int op, Expression arg) {
			this.op = op;
			this.arg = arg;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitUnaryOp(this);
		}
	}

	public static final class BinaryOp extends Expression {
		public final int op;
		public final Expression left;
		public final Expression right;

		public BinaryOp(int op, Expression left, Expression right) {
			this.op = op;
			this.left = left;
			this.right = right;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitBinaryOp(this);
		}
	}

	public static final class Literal extends Expression {
		public final int type;
		public final Object value;

		public Literal(int type, Object value) {
			this.type = type;
			this.value = value;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitLiteral(this);
		}
	}

	public static final class Varargs extends Expression {
		public Varargs() {
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitVarargs(this);
		}
	}

	public static final class FunctionCall extends Expression {
		public final List<Expression> prefix;
		public final String name;
		public final Expression args;

		public FunctionCall(List<Expression> prefix, final String name, Expression args) {
			this.prefix = prefix;
			this.name = name;
			this.args = args;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitFunctionCall(this);
		}
	}

	public static final class Assignment extends Statement {
		public final Expression prefix;
		public final Expression expressions;

		public Assignment(Expression prefix, Expression expressions) {
			this.prefix = prefix;
			this.expressions = expressions;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitAssignment(this);
		}
	}


	public static final class VariableList extends Expression {
		public final List<Expression> variables;

		public VariableList(List<Expression> variables) {
			this.variables = variables;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitVariableList(this);
		}
	}

	public static final class TableConstructor extends Expression {
		public final List<TableField> fields;

		public TableConstructor(List<TableField> fields) {
			this.fields = fields;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitTableConstructor(this);
		}
	}

	public abstract static class TableField extends Expression {
	}

	public static final class IndexField extends TableField {
		public final Expression key;
		public final Expression value;

		public IndexField(Expression key, Expression value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitIndexField(this);
		}
	}

	public static final class ListField extends TableField {
		public final Expression value;

		public ListField(Expression value) {
			this.value = value;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitListField(this);
		}
	}
}
