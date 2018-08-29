package me.jezza.lava.lang.model;

import java.util.List;
import java.util.ListIterator;

import me.jezza.lava.lang.ParseTree;
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
public abstract class AbstractTranslator<T> implements Visitor<T, ParseTree> {

	@SuppressWarnings("unchecked")
	protected <N extends ParseTree> N translate(N node, T userObject) {
		return node != null
				? (N) node.visit(this, userObject)
				: null;
	}

	protected <N extends ParseTree> List<N> translate(List<N> nodes, T userObject) {
		if (nodes == null) {
			return null;
		}
		ListIterator<N> it = nodes.listIterator();
		while (it.hasNext()) {
			N oldNode = it.next();
			N newNode = translate(oldNode, userObject);
			if (oldNode != newNode) {
				it.set(newNode);
			}
		}
		return nodes;
	}

	@Override
	public ParseTree visitBlock(Block value, T userObject) {
		translate(value.statements, userObject);
		return value;
	}

	@Override
	public ParseTree visitExpressionList(ExpressionList value, T userObject) {
		translate(value.list, userObject);
		return value;
	}

	@Override
	public ParseTree visitDoBlock(DoBlock value, T userObject) {
		var old = value.body;
		var body = translate(old, userObject);
		if (old != body) {
			value.body = body;
		}
		return value;
	}

	@Override
	public ParseTree visitRepeatBlock(RepeatBlock value, T userObject) {
		var oldBody = value.body;
		var newBody = translate(oldBody, userObject);
		if (oldBody != newBody) {
			value.body = newBody;
		}
		var oldCondition = value.condition;
		var newCondition = translate(oldCondition, userObject);
		if (oldCondition != newCondition) {
			value.condition = newCondition;
		}
		return value;
	}

	@Override
	public ParseTree visitIfBlock(IfBlock value, T userObject) {
		var oldCondition = value.condition;
		var newCondition = translate(oldCondition, userObject);
		if (oldCondition != newCondition) {
			value.condition = newCondition;
		}
		var oldBlock = value.thenPart;
		var newBlock = translate(oldBlock, userObject);
		if (oldBlock != newBlock) {
			value.thenPart = newBlock;
		}
		var oldElse = value.elsePart;
		var newElse = translate(oldElse, userObject);
		if (oldElse != newElse) {
			value.elsePart = newElse;
		}
		return value;
	}

	@Override
	public ParseTree visitForLoop(ForLoop value, T userObject) {
		var oldLower = value.lowerBound;
		var newLower = translate(oldLower, userObject);
		if (oldLower != newLower) {
			value.lowerBound = newLower;
		}
		var oldUpper = value.upperBound;
		var newUpper = translate(oldUpper, userObject);
		if (oldUpper != newUpper) {
			value.upperBound = newUpper;
		}
		var oldStep = value.step;
		var newStep = translate(oldStep, userObject);
		if (oldStep != newStep) {
			value.step = newStep;
		}
		var oldBody = value.body;
		var newBody = translate(oldBody, userObject);
		if (oldBody != newBody) {
			value.body = newBody;
		}
		return value;
	}

	@Override
	public ParseTree visitForList(ForList value, T userObject) {
		translate(value.nameList, userObject);
		var oldExpressions = value.expressions;
		var newExpressions = translate(oldExpressions, userObject);
		if (oldExpressions != newExpressions) {
			value.expressions = newExpressions;
		}
		var oldBody = value.body;
		var newBody = translate(oldBody, userObject);
		if (oldBody != newBody) {
			value.body = newBody;
		}
		return value;
	}

	@Override
	public ParseTree visitFunctionBody(FunctionBody value, T userObject) {
		translate(value.parameters, userObject);
		var oldBody = value.body;
		var newBody = translate(oldBody, userObject);
		if (oldBody != newBody) {
			value.body = newBody;
		}
		return value;
	}

	@Override
	public ParseTree visitAssignment(Assignment value, T userObject) {
		var oldLeft = value.lhs;
		var newLeft = translate(oldLeft, userObject);
		if (oldLeft != newLeft) {
			value.lhs = newLeft;
		}
		var oldRight = value.rhs;
		var newRight = translate(oldRight, userObject);
		if (oldRight != newRight) {
			value.rhs = newRight;
		}
		return value;
	}

	@Override
	public ParseTree visitUnaryOp(UnaryOp value, T userObject) {
		var oldArg = value.arg;
		var newArg = translate(oldArg, userObject);
		if (oldArg != newArg) {
			value.arg = newArg;
		}
		return value;
	}

	@Override
	public ParseTree visitBinaryOp(BinaryOp value, T userObject) {
		var oldLeft = value.left;
		var newLeft = translate(oldLeft, userObject);
		if (oldLeft != newLeft) {
			value.left = newLeft;
		}
		var oldRight = value.right;
		var newRight = translate(oldRight, userObject);
		if (oldRight != newRight) {
			value.right = newRight;
		}
		return value;
	}

	@Override
	public ParseTree visitFunctionCall(FunctionCall value, T userObject) {
		var oldTarget = value.target;
		var newTarget = translate(oldTarget, userObject);
		if (oldTarget != newTarget) {
			value.target = newTarget;
		}
		var oldArgs = value.args;
		var newArgs = translate(oldArgs, userObject);
		if (oldArgs != newArgs) {
			value.args = newArgs;
		}
		return value;
	}

	@Override
	public ParseTree visitTableConstructor(TableConstructor value, T userObject) {
		translate(value.fields, userObject);
		return value;
	}

	@Override
	public ParseTree visitTableField(TableField value, T userObject) {
		var oldKey = value.key;
		var newKey = translate(oldKey, userObject);
		if (oldKey != newKey) {
			value.key = newKey;
		}
		var oldValue = value.value;
		var newValue = translate(oldValue, userObject);
		if (oldValue != newValue) {
			value.value = newValue;
		}
		return value;
	}

	@Override
	public ParseTree visitReturnStatement(ReturnStatement value, T userObject) {
		var oldValue = value.expressions;
		var newValue = translate(oldValue, userObject);
		if (oldValue != newValue) {
			value.expressions = newValue;
		}
		return value;
	}

	@Override
	public ParseTree visitLiteral(Literal value, T userObject) {
		return value;
	}

	@Override
	public ParseTree visitName(Name value, T userObject) {
		return value;
	}

	@Override
	public ParseTree visitBreak(Break value, T userObject) {
		return value;
	}

	@Override
	public ParseTree visitGoto(Goto value, T userObject) {
		return value;
	}

	@Override
	public ParseTree visitLabel(Label value, T userObject) {
		return value;
	}

	@Override
	public ParseTree visitVarargs(Varargs value, T userObject) {
		return value;
	}
}
