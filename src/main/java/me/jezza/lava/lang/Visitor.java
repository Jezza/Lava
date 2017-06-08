package me.jezza.lava.lang;

import me.jezza.lava.lang.ast.Tree.AssignStatement;
import me.jezza.lava.lang.ast.Tree.Block;
import me.jezza.lava.lang.ast.Tree.Break;
import me.jezza.lava.lang.ast.Tree.DoBlock;
import me.jezza.lava.lang.ast.Tree.Expression;
import me.jezza.lava.lang.ast.Tree.ExpressionList;
import me.jezza.lava.lang.ast.Tree.ForLoop;
import me.jezza.lava.lang.ast.Tree.ForLoopIn;
import me.jezza.lava.lang.ast.Tree.FunctionBody;
import me.jezza.lava.lang.ast.Tree.FunctionName;
import me.jezza.lava.lang.ast.Tree.FunctionStatement;
import me.jezza.lava.lang.ast.Tree.Goto;
import me.jezza.lava.lang.ast.Tree.IfBlock;
import me.jezza.lava.lang.ast.Tree.Label;
import me.jezza.lava.lang.ast.Tree.LocalFunctionStatement;
import me.jezza.lava.lang.ast.Tree.ParameterList;
import me.jezza.lava.lang.ast.Tree.RepeatBlock;
import me.jezza.lava.lang.ast.Tree.ReturnStatement;
import me.jezza.lava.lang.ast.Tree.WhileLoop;

/**
 * @author Jezza
 */
public interface Visitor {

	void visitAssignStatement(AssignStatement value);

	void visitExpressionList(ExpressionList value);

	void visitLabel(Label value);

	void visitLocalFunctionStatement(LocalFunctionStatement value);

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
	void visitForLoopIn(ForLoopIn value);
	void visitReturnStatement(ReturnStatement value);

	void visitExpression(Expression value);
}
