package me.jezza.lava.lang;

import me.jezza.lava.lang.ast.Tree.Assignment;
import me.jezza.lava.lang.ast.Tree.BinaryOp;
import me.jezza.lava.lang.ast.Tree.Block;
import me.jezza.lava.lang.ast.Tree.Break;
import me.jezza.lava.lang.ast.Tree.DoBlock;
import me.jezza.lava.lang.ast.Tree.ExpressionList;
import me.jezza.lava.lang.ast.Tree.ForLoop;
import me.jezza.lava.lang.ast.Tree.ForList;
import me.jezza.lava.lang.ast.Tree.FunctionBody;
import me.jezza.lava.lang.ast.Tree.FunctionCall;
import me.jezza.lava.lang.ast.Tree.FunctionName;
import me.jezza.lava.lang.ast.Tree.FunctionStatement;
import me.jezza.lava.lang.ast.Tree.Goto;
import me.jezza.lava.lang.ast.Tree.IfBlock;
import me.jezza.lava.lang.ast.Tree.IndexField;
import me.jezza.lava.lang.ast.Tree.Label;
import me.jezza.lava.lang.ast.Tree.ListField;
import me.jezza.lava.lang.ast.Tree.Literal;
import me.jezza.lava.lang.ast.Tree.LocalFunction;
import me.jezza.lava.lang.ast.Tree.LocalStatement;
import me.jezza.lava.lang.ast.Tree.ParameterList;
import me.jezza.lava.lang.ast.Tree.RepeatBlock;
import me.jezza.lava.lang.ast.Tree.ReturnStatement;
import me.jezza.lava.lang.ast.Tree.TableConstructor;
import me.jezza.lava.lang.ast.Tree.UnaryOp;
import me.jezza.lava.lang.ast.Tree.Varargs;
import me.jezza.lava.lang.ast.Tree.VariableList;
import me.jezza.lava.lang.ast.Tree.WhileLoop;

/**
 * @author Jezza
 */
public interface Visitor {

	void visitFunctionCall(FunctionCall value);
	void visitAssignment(Assignment value);

	void visitExpressionList(ExpressionList value);

	void visitLabel(Label value);

	void visitLocalStatement(LocalStatement value);
	void visitLocalFunction(LocalFunction value);

	void visitFunctionStatement(FunctionStatement value);
	void visitFunctionName(FunctionName value);
	void visitFunctionBody(FunctionBody value);
	void visitParameterList(ParameterList value);

	void visitBlock(Block value);

	void visitGoto(Goto value);
	void visitBreak(Break value);

	void visitDoBlock(DoBlock value);
	void visitWhileLoop(WhileLoop value);
	void visitRepeatBlock(RepeatBlock value);
	void visitIfBlock(IfBlock value);
	void visitForLoop(ForLoop value);
	void visitForLoopIn(ForList value);
	void visitReturnStatement(ReturnStatement value);

	void visitUnaryOp(UnaryOp value);
	void visitBinaryOp(BinaryOp value);

	void visitLiteral(Literal value);
	void visitVarargs(Varargs value);

	void visitTableConstructor(TableConstructor value);

	void visitIndexField(IndexField value);
	void visitListField(ListField value);

	void visitVariableList(VariableList value);
}
