package me.jezza.lava.lang;

import java.util.Iterator;

import me.jezza.lava.lang.ast.ParseTree;
import me.jezza.lava.lang.ast.ParseTree.Assignment;
import me.jezza.lava.lang.ast.ParseTree.BinaryOp;
import me.jezza.lava.lang.ast.ParseTree.Block;
import me.jezza.lava.lang.ast.ParseTree.Break;
import me.jezza.lava.lang.ast.ParseTree.DoBlock;
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
import me.jezza.lava.lang.ast.ParseTree.Variable;
import me.jezza.lava.lang.ast.ParseTree.WhileLoop;
import me.jezza.lava.lang.interfaces.Visitor.EVisitor;

/**
 * @author Jezza
 */
public final class ASTPrinter implements EVisitor {
	private final StringBuilder text;
	private final int increment;

	private int level;


	public ASTPrinter(int increment) {
		text = new StringBuilder();
		this.increment = increment;
	}

	private void indent() {
		if (text.charAt(text.length() - 1) == '\n') {
			for (int i = 0; i < level; i++) {
				text.append(' ');
			}
		}
	}

	private void shift(int delta) {
		level += delta;
	}

	private void visit(Iterator<? extends ParseTree> it, String glue) {
		visit(it, glue, null);
	}

	private void visit(Iterator<? extends ParseTree> it, String prefix, String suffix) {
		if (it.hasNext()) {
			ParseTree next = it.next();
			next.visit(this);
			while (it.hasNext()) {
				text.append(prefix);
				next = it.next();
				next.visit(this);
				if (suffix != null) {
					text.append(suffix);
				}
			}
		}
	}

	private void join(Iterator<String> it, String glue) {
		join(it, glue, null);
	}

	private void join(Iterator<String> it, String prefix, String suffix) {
		if (it.hasNext()) {
			text.append(it.next());
			while (it.hasNext()) {
				text.append(prefix).append(it.next());
				if (suffix != null) {
					text.append(suffix);
				}
			}
		}
	}

	@Override
	public Void visitBlock(Block value, Void userObject) {
		text.append("{\n");
		shift(increment);
		for (Statement statement : value.statements) {
			statement.visit(this);
			text.append('\n');
		}
		shift(-increment);
		indent();
		text.append("}\n");
		return null;
	}

	@Override
	public Void visitVariable(Variable variable, Void userObject) {
		indent();
		text.append("var ").append(variable.name);
		return null;
	}

	@Override
	public Void visitAssignment(Assignment value, Void userObject) {
		if (value.lhs != null) {
			value.lhs.visit(this);
			text.append(" = ");
		}
		value.rhs.visit(this);
		return null;
	}

	@Override
	public Void visitFunctionCall(FunctionCall value, Void userObject) {
		indent();
		visit(value.prefix.iterator(), "[", "]");
		if (value.name != null) {
			text.append(':').append(value.name);
		}
		text.append('(');
		value.args.visit(this);
		text.append(')');
		return null;
	}

	@Override
	public Void visitExpressionList(ExpressionList value, Void userObject) {
		visit(value.list.iterator(), ", ");
		return null;
	}

	@Override
	public Void visitLocalStatement(LocalStatement value, Void userObject) {
		indent();
		text.append("local ");
		join(value.lhs.iterator(), ", ");
		text.append(" = ");
		value.rhs.visit(this);
		text.append(';');
		return null;
	}

	@Override
	public Void visitLocalFunction(LocalFunction value, Void userObject) {
		text.append("local function");
		return null;
	}

//	@Override
//	public Void visitFunctionStatement(FunctionStatement value, Void userObject) {
//		indent();
//		value.name.visit(this);
//		value.body.visit(this);
//		return null;
//	}
//
//	@Override
//	public Void visitFunctionName(FunctionName value, Void userObject) {
//		text.append(value.first);
//		if (!value.nested.isEmpty()) {
//			for (String name : value.nested) {
//				text.append(',').append(name);
//			}
//		}
//		return null;
//	}

	@Override
	public Void visitFunctionBody(FunctionBody value, Void userObject) {
		text.append(" function (");
		value.parameterList.visit(this);
		text.append(')');
		text.append(" ");
		value.body.visit(this);
		return null;
	}

	@Override
	public Void visitParameterList(ParameterList value, Void userObject) {
		join(value.nameList.iterator(), ", ");
		if (value.varargs) {
			if (!value.nameList.isEmpty()) {
				text.append(", ");
			}
			text.append("...");
		}
		return null;
	}

	@Override
	public Void visitLabel(Label value, Void userObject) {
		text.append("label");
		return null;
	}

	@Override
	public Void visitGoto(Goto value, Void userObject) {
		text.append("goto");
		return null;
	}

	@Override
	public Void visitBreak(Break value, Void userObject) {
		text.append("break");
		return null;
	}

	@Override
	public Void visitDoBlock(DoBlock value, Void userObject) {
		text.append("do");
		return null;
	}

	@Override
	public Void visitWhileLoop(WhileLoop value, Void userObject) {
		text.append("while");
		return null;
	}

	@Override
	public Void visitRepeatBlock(RepeatBlock value, Void userObject) {
		text.append("repeat");
		return null;
	}

	@Override
	public Void visitIfBlock(IfBlock value, Void userObject) {
		text.append("if");
		return null;
	}

	@Override
	public Void visitForLoop(ForLoop value, Void userObject) {
		text.append("for");
		return null;
	}

	@Override
	public Void visitForList(ForList value, Void userObject) {
		text.append("for list");
		return null;
	}

	@Override
	public Void visitReturnStatement(ReturnStatement value, Void userObject) {
		text.append("return");
		return null;
	}

	@Override
	public Void visitUnaryOp(UnaryOp value, Void userObject) {
		text.append("unary");
		return null;
	}

	@Override
	public Void visitBinaryOp(BinaryOp value, Void userObject) {
		text.append("binary");
		return null;
	}

	@Override
	public Void visitLiteral(Literal value, Void userObject) {
		switch (value.type) {
			case Literal.TRUE:
				text.append("true");
				break;
			case Literal.FALSE:
				text.append("false");
				break;
			case Literal.NIL:
				text.append("nil");
				break;
			case Literal.INTEGER:
			case Literal.DOUBLE:
			case Literal.NAMESPACE:
				text.append(value.value);
				break;
			case Literal.STRING:
				text.append('"');
				text.append(value.value);
				text.append('"');
				break;
		}
		return null;
	}

	@Override
	public Void visitVarargs(Varargs value, Void userObject) {
		return null;
	}

	@Override
	public Void visitTableConstructor(TableConstructor value, Void userObject) {
		return null;
	}

	@Override
	public Void visitTableField(TableField value, Void userObject) {
		return null;
	}

	public static String print(Block block) {
		ASTPrinter printer = new ASTPrinter(4);
		block.visit(printer);
		return printer.text.toString();
	}
}
