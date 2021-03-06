package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.FLAG_ASSIGNMENT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;

import java.util.Iterator;

import me.jezza.lava.lang.ParseTree.Assignment;
import me.jezza.lava.lang.ParseTree.BinaryOp;
import me.jezza.lava.lang.ParseTree.Block;
import me.jezza.lava.lang.ParseTree.Break;
import me.jezza.lava.lang.ParseTree.DoBlock;
import me.jezza.lava.lang.ParseTree.ExpressionList;
import me.jezza.lava.lang.ParseTree.ForList;
import me.jezza.lava.lang.ParseTree.ForLoop;
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
import me.jezza.lava.lang.interfaces.Visitor.EVisitor;

/**
 * @author Jezza
 */
public final class ASTPrinter implements EVisitor {
	private final StringBuilder text;
	private final int increment;

	private int level;

	private ASTPrinter(int increment) {
		text = new StringBuilder();
		this.increment = increment;
	}

	private void indent() {
		if (text.length() == 0 || text.charAt(text.length() - 1) == '\n') {
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
		shift(increment);
		indent();
		for (Statement statement : value.statements) {
			statement.visit(this);
			text.append('\n');
		}
		shift(-increment);
		indent();
		return null;
	}

	@Override
	public Void visitAssignment(Assignment value, Void userObject) {
		indent();
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
		value.target.visit(this);
		if (value.name != null) {
			text.append(':').append(value.name);
		}
		text.append('<');
		text.append(value.expectedResults);
		text.append(">(");
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
	public Void visitFunctionBody(FunctionBody value, Void userObject) {
		text.append("function(");
		visit(value.parameters.iterator(), ", ");
		if (value.varargs) {
			text.append(value.parameters.isEmpty() ? "..." : ", ...");
		}
		text.append(')');
		text.append(" \n");
		value.body.visit(this);
		text.append("end");
		return null;
	}

	@Override
	public Void visitLabel(Label value, Void userObject) {
		indent();
		text.append("::").append(value.name).append('<').append(value.hashCode()).append(">::");
		return null;
	}

	@Override
	public Void visitGoto(Goto value, Void userObject) {
		indent();
		text.append("goto <");
		text.append(value.resolvedLabel != null
				? value.resolvedLabel.hashCode()
				: -1);
		text.append('>').append(value.label).append(';');
		return null;
	}

	@Override
	public Void visitBreak(Break value, Void userObject) {
		text.append("break");
		return null;
	}

	@Override
	public Void visitDoBlock(DoBlock value, Void userObject) {
		indent();
		text.append("do\n");
		value.body.visit(this, userObject);
		text.append("end");
		return null;
	}

	@Override
	public Void visitRepeatBlock(RepeatBlock value, Void userObject) {
		indent();
		text.append("repeat\n");
		value.body.visit(this, userObject);
		text.append("until (");
		value.condition.visit(this, userObject);
		text.append(')');
		return null;
	}

	@Override
	public Void visitIfBlock(IfBlock value, Void userObject) {
		indent();
		text.append("if (");
		value.condition.visit(this);
		text.append(") then\n");
		value.thenPart.visit(this);
		if (value.elsePart != null) {
			text.append('\n');
			indent();
			text.append("else");
			text.append('\n');
			value.elsePart.visit(this);
		}
		indent();
		text.append("end");
		return null;
	}

	@Override
	public Void visitForLoop(ForLoop value, Void userObject) {
		text.append("for ").append(value.name).append(" = ")
				.append(value.lowerBound)
				.append(", ").append(value.upperBound)
				.append(", ").append(value.step)
				.append(" do\n");
		value.body.visit(this);
		text.append("end");
		return null;
	}

	@Override
	public Void visitForList(ForList value, Void userObject) {
		text.append("[[FOR LIST]]");
		return null;
	}

	@Override
	public Void visitReturnStatement(ReturnStatement value, Void userObject) {
		indent();
		text.append("return ");
		value.expressions.visit(this);
		text.append(';');
		return null;
	}

	@Override
	public Void visitUnaryOp(UnaryOp value, Void userObject) {
		switch (value.op) {
			case UnaryOp.OP_MINUS:
				text.append('-');
				break;
			case UnaryOp.OP_NOT:
				text.append("not");
				break;
			case UnaryOp.OP_LEN:
				text.append('#');
				break;
			case UnaryOp.OP_TO_NUMBER:
				text.append("to_number!");
				break;
			case UnaryOp.OP_TO_STRING:
				text.append("to_string!");
				break;
			case UnaryOp.OP_ERROR:
				text.append("error!");
				break;
			default:
				throw new IllegalStateException("Unsupported op: " + value.op);
		}
		text.append('(');
		value.arg.visit(this);
		text.append(')');
		return null;
	}

	@Override
	public Void visitBinaryOp(BinaryOp value, Void userObject) {
		boolean indexed = value.op == BinaryOp.OP_INDEXED;
		if (value.is(FLAG_ASSIGNMENT)) {
			assert indexed;
			text.append("set_table(");
			value.left.visit(this);
			text.append(',');
			value.right.visit(this);
			text.append(", &VAL)");
			return null;
		}

		value.left.visit(this);
		switch (value.op) {
			case BinaryOp.OP_ADD:
				text.append(" + ");
				break;
			case BinaryOp.OP_SUB:
				text.append(" - ");
				break;
			case BinaryOp.OP_MUL:
				text.append(" * ");
				break;
			case BinaryOp.OP_DIV:
				text.append(" / ");
				break;
			case BinaryOp.OP_MOD:
				text.append(" & ");
				break;
			case BinaryOp.OP_POW:
				text.append(" ^ ");
				break;
			case BinaryOp.OP_CONCAT:
				text.append(" .. ");
				break;
			case BinaryOp.OP_NE:
				text.append(" ~= ");
				break;
			case BinaryOp.OP_EQ:
				text.append(" == ");
				break;
			case BinaryOp.OP_LT:
				text.append(" < ");
				break;
			case BinaryOp.OP_LE:
				text.append(" <= ");
				break;
			case BinaryOp.OP_GT:
				text.append(" > ");
				break;
			case BinaryOp.OP_GE:
				text.append(" >= ");
				break;
			case BinaryOp.OP_AND:
				text.append(" and ");
				break;
			case BinaryOp.OP_OR:
				text.append(" or ");
				break;
			case BinaryOp.OP_INDEXED:
				text.append('[');
				break;
			default:
				throw new IllegalStateException("BinOp: " + value.op);
		}
		value.right.visit(this);
		if (indexed) {
			text.append(']');
		}
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
	public Void visitName(Name value, Void userObject) {
		boolean local = value.is(FLAG_LOCAL);
		boolean assign = value.is(FLAG_ASSIGNMENT);
		String rep = value.value + '<' + value.level + ',' + value.index + '>';
		if (local && assign) {
			text.append("local(set(").append(rep).append("))");
		} else if (local) {
			text.append("local(").append(rep).append(')');
		} else if (assign) {
			text.append("set(").append(rep).append(')');
		} else {
			text.append(rep);
		}
		return null;
	}

	@Override
	public Void visitVarargs(Varargs value, Void userObject) {
		text.append('<').append(value.expectedResults).append('>').append("...");
		return null;
	}

	@Override
	public Void visitTableConstructor(TableConstructor value, Void userObject) {
		text.append('{');
		visit(value.fields.iterator(), ",\n");
		text.append('}');
		return null;
	}

	@Override
	public Void visitTableField(TableField value, Void userObject) {
		indent();
		value.key.visit(this);
		text.append('=');
		value.value.visit(this);
		return null;
	}

	public static String print(ParseTree node) {
		ASTPrinter printer = new ASTPrinter(4);
		node.visit(printer);
		return printer.text.toString();
	}
}
