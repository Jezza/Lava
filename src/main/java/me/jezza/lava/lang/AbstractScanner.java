package me.jezza.lava.lang;

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
import me.jezza.lava.lang.ParseTree.TableConstructor;
import me.jezza.lava.lang.ParseTree.TableField;
import me.jezza.lava.lang.ParseTree.UnaryOp;
import me.jezza.lava.lang.ParseTree.Varargs;
import me.jezza.lava.lang.interfaces.Visitor;

/**
 * @author Jezza
 */
public class AbstractScanner<P, R> implements Visitor<P, R> {

	public R scan(ParseTree node, P userObject) {
		return node != null
				? node.visit(this, userObject)
				: null;
	}

	public R scanThenReduce(ParseTree node, P userObject, R value) {
		return reduce(scan(node, userObject), value, userObject);
	}

	public R scan(Iterable<? extends ParseTree> nodes, P userObject) {
		if (nodes == null) {
			return null;
		}
		Iterator<? extends ParseTree> it = nodes.iterator();
		if (!it.hasNext()) {
			return null;
		}
		ParseTree node = it.next();
		R value = scan(node, userObject);
		while (it.hasNext()) {
			value = scanThenReduce(it.next(), userObject, value);
		}
		return value;
	}

	public R scanThenReduce(Iterable<? extends ParseTree> nodes, P userObject, R value) {
		return reduce(scan(nodes, userObject), value, userObject);
	}

	public R reduce(R left, R right, P userObject) {
		return left;
	}

	@Override
	public R visitBlock(Block value, P userObject) {
		return scan(value.statements, userObject);
	}

	@Override
	public R visitExpressionList(ExpressionList value, P userObject) {
		return scan(value.list, userObject);
	}

	@Override
	public R visitDoBlock(DoBlock value, P userObject) {
		return scan(value.body, userObject);
	}

	@Override
	public R visitRepeatBlock(RepeatBlock value, P userObject) {
		R returnValue = scan(value.body, userObject);
		return scanThenReduce(value.condition, userObject, returnValue);
	}

	@Override
	public R visitIfBlock(IfBlock value, P userObject) {
		R returnValue = scan(value.condition, userObject);
		returnValue = scanThenReduce(value.thenPart, userObject, returnValue);
		return scanThenReduce(value.elsePart, userObject, returnValue);
	}

	@Override
	public R visitForLoop(ForLoop value, P userObject) {
		R returnValue = scan(value.lowerBound, userObject);
		returnValue = scanThenReduce(value.upperBound, userObject, returnValue);
		returnValue = scanThenReduce(value.step, userObject, returnValue);
		return scanThenReduce(value.body, userObject, returnValue);
	}

	@Override
	public R visitForList(ForList value, P userObject) {
		R returnValue = scan(value.nameList, userObject);
		returnValue = scanThenReduce(value.expressions, userObject, returnValue);
		return scanThenReduce(value.body, userObject, returnValue);
	}

	@Override
	public R visitFunctionBody(FunctionBody value, P userObject) {
		R returnValue = scan(value.parameters, userObject);
		return scanThenReduce(value.body, userObject, returnValue);
	}

	@Override
	public R visitAssignment(Assignment value, P userObject) {
		R returnValue = scan(value.rhs, userObject);
		if (value.lhs != null) {
			return scanThenReduce(value.lhs, userObject, returnValue);
		}
		return returnValue;
	}

	@Override
	public R visitUnaryOp(UnaryOp value, P userObject) {
		return scan(value.arg, userObject);
	}

	@Override
	public R visitBinaryOp(BinaryOp value, P userObject) {
		R returnValue = scan(value.left, userObject);
		return scanThenReduce(value.right, userObject, returnValue);
	}

	@Override
	public R visitFunctionCall(FunctionCall value, P userObject) {
		R returnValue = scan(value.target, userObject);
		return scanThenReduce(value.args, userObject, returnValue);
	}

	@Override
	public R visitTableConstructor(TableConstructor value, P userObject) {
		return scan(value.fields, userObject);
	}

	@Override
	public R visitTableField(TableField value, P userObject) {
		R returnValue = scan(value.key, userObject);
		return scanThenReduce(value.value, userObject, returnValue);
	}

	@Override
	public R visitReturnStatement(ReturnStatement value, P userObject) {
		return scan(value.expressions, userObject);
	}

	@Override
	public R visitLiteral(Literal value, P userObject) {
		return null;
	}

	@Override
	public R visitName(Name value, P userObject) {
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
