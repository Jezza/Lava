package me.jezza.lava.lang.interfaces;

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
import me.jezza.lava.lang.ast.ParseTree.LocalStatement;
import me.jezza.lava.lang.ast.ParseTree.ParameterList;
import me.jezza.lava.lang.ast.ParseTree.RepeatBlock;
import me.jezza.lava.lang.ast.ParseTree.ReturnStatement;
import me.jezza.lava.lang.ast.ParseTree.TableConstructor;
import me.jezza.lava.lang.ast.ParseTree.TableField;
import me.jezza.lava.lang.ast.ParseTree.UnaryOp;
import me.jezza.lava.lang.ast.ParseTree.Varargs;
import me.jezza.lava.lang.ast.ParseTree.WhileLoop;

/**
 * @author Jezza
 */
public interface Visitor<P, R> {
	R visitBlock(Block value, P userObject);

	R visitAssignment(Assignment value, P userObject);

	R visitFunctionCall(FunctionCall value, P userObject);

	R visitExpressionList(ExpressionList value, P userObject);

	R visitLocalStatement(LocalStatement value, P userObject);

	R visitFunctionBody(FunctionBody value, P userObject);

	R visitParameterList(ParameterList value, P userObject);

	R visitLabel(Label value, P userObject);

	R visitGoto(Goto value, P userObject);

	R visitBreak(Break value, P userObject);

	R visitDoBlock(DoBlock value, P userObject);

	R visitWhileLoop(WhileLoop value, P userObject);

	R visitRepeatBlock(RepeatBlock value, P userObject);

	R visitIfBlock(IfBlock value, P userObject);

	R visitForLoop(ForLoop value, P userObject);

	R visitForList(ForList value, P userObject);

	R visitReturnStatement(ReturnStatement value, P userObject);

	R visitUnaryOp(UnaryOp value, P userObject);

	R visitBinaryOp(BinaryOp value, P userObject);

	R visitLiteral(Literal value, P userObject);

	R visitVarargs(Varargs value, P userObject);

	R visitTableConstructor(TableConstructor value, P userObject);

	R visitTableField(TableField value, P userObject);

	interface PVisitor<T> extends Visitor<T, Void> {
	}

	interface RVisitor<T> extends Visitor<Void, T> {
	}

	interface EVisitor extends Visitor<Void, Void> {
	}
}
