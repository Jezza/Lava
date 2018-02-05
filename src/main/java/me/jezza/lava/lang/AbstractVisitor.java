package me.jezza.lava.lang;

import me.jezza.lava.lang.ast.ParseTree.Assignment;
import me.jezza.lava.lang.ast.ParseTree.BinaryOp;
import me.jezza.lava.lang.ast.ParseTree.Block;
import me.jezza.lava.lang.ast.ParseTree.Break;
import me.jezza.lava.lang.ast.ParseTree.DoBlock;
import me.jezza.lava.lang.ast.ParseTree.Expression;
import me.jezza.lava.lang.ast.ParseTree.ExpressionList;
import me.jezza.lava.lang.ast.ParseTree.ForList;
import me.jezza.lava.lang.ast.ParseTree.ForLoop;
import me.jezza.lava.lang.ast.ParseTree.FunctionBody;
import me.jezza.lava.lang.ast.ParseTree.FunctionCall;
import me.jezza.lava.lang.ast.ParseTree.Goto;
import me.jezza.lava.lang.ast.ParseTree.IfBlock;
import me.jezza.lava.lang.ast.ParseTree.Label;
import me.jezza.lava.lang.ast.ParseTree.Literal;
import me.jezza.lava.lang.ast.ParseTree.LocalStatement;
import me.jezza.lava.lang.ast.ParseTree.RepeatBlock;
import me.jezza.lava.lang.ast.ParseTree.ReturnStatement;
import me.jezza.lava.lang.ast.ParseTree.Statement;
import me.jezza.lava.lang.ast.ParseTree.TableConstructor;
import me.jezza.lava.lang.ast.ParseTree.TableField;
import me.jezza.lava.lang.ast.ParseTree.UnaryOp;
import me.jezza.lava.lang.ast.ParseTree.Varargs;
import me.jezza.lava.lang.ast.ParseTree.WhileLoop;
import me.jezza.lava.lang.interfaces.Visitor;

/**
 * @author Jezza
 */
public class AbstractVisitor<P, R> implements Visitor<P, R> {
	@Override
	public R visitBlock(Block value, P userObject) {
		for (Statement statement : value.statements) {
			statement.visit(this, userObject);
		}
		return null;
	}

	@Override
	public R visitExpressionList(ExpressionList value, P userObject) {
		for (Expression expression : value.list) {
			expression.visit(this, userObject);
		}
		return null;
	}

	@Override
	public R visitDoBlock(DoBlock value, P userObject) {
		return value.body.visit(this, userObject);
	}

	@Override
	public R visitWhileLoop(WhileLoop value, P userObject) {
		value.condition.visit(this, userObject);
		value.body.visit(this, userObject);
		return null;
	}

	@Override
	public R visitRepeatBlock(RepeatBlock value, P userObject) {
		value.condition.visit(this, userObject);
		value.body.visit(this, userObject);
		return null;
	}

	@Override
	public R visitIfBlock(IfBlock value, P userObject) {
		value.condition.visit(this, userObject);
		value.thenPart.visit(this, userObject);
		value.elsePart.visit(this, userObject);
		return null;
	}

	@Override
	public R visitForLoop(ForLoop value, P userObject) {
		value.lowerBound.visit(this, userObject);
		value.upperBound.visit(this, userObject);
		value.step.visit(this, userObject);
		value.body.visit(this, userObject);
		return null;
	}

	@Override
	public R visitForList(ForList value, P userObject) {
		value.expList.visit(this, userObject);
		value.body.visit(this, userObject);
		return null;
	}

	@Override
	public R visitFunctionBody(FunctionBody value, P userObject) {
		return value.body.visit(this, userObject);
	}

	@Override
	public R visitLocalStatement(LocalStatement value, P userObject) {
		return value.rhs.visit(this, userObject);
	}

	@Override
	public R visitAssignment(Assignment value, P userObject) {
		value.lhs.visit(this, userObject);
		value.rhs.visit(this, userObject);
		return null;
	}

	@Override
	public R visitUnaryOp(UnaryOp value, P userObject) {
		value.arg.visit(this, userObject);
		return null;
	}

	@Override
	public R visitBinaryOp(BinaryOp value, P userObject) {
		value.left.visit(this, userObject);
		value.right.visit(this, userObject);
		return null;
	}

	@Override
	public R visitFunctionCall(FunctionCall value, P userObject) {
		value.target.visit(this, userObject);
		value.args.visit(this, userObject);
		return null;
	}

	@Override
	public R visitTableConstructor(TableConstructor value, P userObject) {
		for (TableField field : value.fields) {
			field.key.visit(this, userObject);
			field.value.visit(this, userObject);
		}
		return null;
	}

	@Override
	public R visitTableField(TableField value, P userObject) {
		value.key.visit(this, userObject);
		value.value.visit(this, userObject);
		return null;
	}

	@Override
	public R visitReturnStatement(ReturnStatement value, P userObject) {
		return value.exprs.visit(this, userObject);
	}

	@Override
	public R visitLiteral(Literal value, P userObject) {
		return null;
	}

	@Override
	public R visitBreak(Break value, P userObject) {
		return null;
	}

	@Override
	public R visitGoto(Goto value, P userObject) {
		return null;
	}

	@Override
	public R visitLabel(Label value, P userObject) {
		return null;
	}

	@Override
	public R visitVarargs(Varargs value, P userObject) {
		return null;
	}
}
