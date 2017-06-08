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

	public static final class AssignStatement extends Statement {
		public final Statement varList;
		public final Statement expList;

		public AssignStatement(Statement varList, Statement expList) {
			this.varList = varList;
			this.expList = expList;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitAssignStatement(this);
		}
	}

//	public static final class FunctionCall extends Statement {
//	}

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

	public static final class ForLoopIn extends Statement {
		public final List<String> nameList;
		public final Statement expList;

		public ForLoopIn(List<String> nameList, Statement expList) {
			this.nameList = nameList;
			this.expList = expList;
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

	public static final class FunctionBody extends Statement {
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

	public static final class LocalFunctionStatement extends Statement {
		public final String name;
		public final FunctionBody body;

		protected LocalFunctionStatement(String name, FunctionBody body) {
			this.name = name;
			this.body = body;
		}

		@Override
		public void visit(Visitor visitor) {
			visitor.visitLocalFunctionStatement(this);
		}
	}

	public static final class ReturnStatement extends Statement {
		public final ExpressionList expList;

		public ReturnStatement(ExpressionList expList) {
			this.expList = expList;
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

	public abstract static class Variable extends Statement {

	}

//	public static final class NamedVariable extends Variable {
//		public final String name;
//
//		public NamedVariable(String name) {
//			this.name = name;
//		}
//	}
//
//	public static final class VariableList extends Statement {
//		public final List<Variable> vars;
//
//		public VariableList(List<Variable> vars) {
//			this.vars = vars;
//		}
//	}

	public static final class ExpressionList extends Statement {
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

	}

//	public static class UnaryOP extends Expression {
//	}
//
//	public static class BinaryOP extends Expression {
//		public final int op;
//		public final Expression left;
//		public final Expression right;
//
//		public BinaryOP(int op, Expression left, Expression right) {
//			this.op = op;
//			this.left = left;
//			this.right = right;
//		}
//	}
}
