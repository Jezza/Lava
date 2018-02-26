package me.jezza.lava.lang.interfaces;

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

/**
 * @author Jezza
 */
public interface Visitor<P, R> {
	R visitBlock(Block value, P userObject);

	R visitExpressionList(ExpressionList value, P userObject);

	R visitDoBlock(DoBlock value, P userObject);

	R visitRepeatBlock(RepeatBlock value, P userObject);

	R visitIfBlock(IfBlock value, P userObject);

	R visitForLoop(ForLoop value, P userObject);

	R visitForList(ForList value, P userObject);

	R visitFunctionBody(FunctionBody value, P userObject);

	R visitAssignment(Assignment value, P userObject);

	R visitUnaryOp(UnaryOp value, P userObject);

	R visitBinaryOp(BinaryOp value, P userObject);

	R visitFunctionCall(FunctionCall value, P userObject);

	R visitTableConstructor(TableConstructor value, P userObject);

	R visitTableField(TableField value, P userObject);

	R visitReturnStatement(ReturnStatement value, P userObject);

	R visitLiteral(Literal value, P userObject);

	R visitName(Name value, P userObject);

	R visitBreak(Break value, P userObject);

	R visitGoto(Goto value, P userObject);

	R visitLabel(Label value, P userObject);

	R visitVarargs(Varargs value, P userObject);

	interface PVisitor<T> extends Visitor<T, Void> {
	}

	interface RVisitor<T> extends Visitor<Void, T> {
	}

	interface EVisitor extends Visitor<Void, Void> {
	}
}
