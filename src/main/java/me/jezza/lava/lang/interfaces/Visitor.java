package me.jezza.lava.lang.interfaces;

import me.jezza.lava.lang.ast.Tree.*;

/**
 * @author Jezza
 */
public interface Visitor<P, R> {
	R visitBlock(Block value, P userObject);

	R visitVariable(Variable variable, P userObject);

	R visitAssignment(Assignment value, P userObject);

	R visitFunctionCall(FunctionCall value, P userObject);

	R visitExpressionList(ExpressionList value, P userObject);

	R visitLocalStatement(LocalStatement value, P userObject);

	R visitLocalFunction(LocalFunction value, P userObject);

	R visitFunctionStatement(FunctionStatement value, P userObject);

	R visitFunctionName(FunctionName value, P userObject);

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
