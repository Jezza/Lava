package me.jezza.lava.lang;

import java.util.List;

import me.jezza.lava.lang.LavaEmitter.Scope;
import me.jezza.lava.lang.ast.ParseTree;
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
import me.jezza.lava.lang.emitter.ByteCodeWriter;
import me.jezza.lava.lang.emitter.ConstantPool;
import me.jezza.lava.lang.interfaces.Visitor.PVisitor;
import me.jezza.lava.runtime.Interpreter.LuaChunk;
import me.jezza.lava.runtime.OpCode;

/**
 * @author Jezza
 */
public final class LavaEmitter implements PVisitor<Scope> {

	public static LuaChunk emit(String name, Block block) {
		LavaEmitter emitter = new LavaEmitter();
		Scope scope = new Scope(name);
		block.visit(emitter, scope);
		return scope.build();
	}

	static final class Scope {
		final String name;
		final Scope previous;
		final ByteCodeWriter w;
		final ConstantPool pool;
		final ConstantPool locals;

		int top;
		int max;

		Scope(String name) {
			this(name, null);
		}

		private Scope(String name, Scope previous) {
			this.name = name;
			this.previous = previous;
			this.w = new ByteCodeWriter();
			this.pool = new ConstantPool();
			locals = new ConstantPool();
		}

		public Scope newScope(String name) {
			return new Scope(name, this);
		}

		public void reserve(int count) {
			top += count;
			if (top > max) {
				max = top;
			}
		}

		public int registerLocal(String name) {
			return locals.add(name);
		}

		public LuaChunk build() {
			LuaChunk chunk = new LuaChunk(name);
			chunk.constants = pool.build();
			if (w.mark() == 0 || w.get(w.mark() - 1) != OpCode.RET) {
				w.write2(OpCode.RET, 0);
			}
			chunk.code = w.code();
			chunk.maxStackSize = max;
			// chunk.chunks = chunks.toArray(new LuaChunk[0]);
			return chunk;
		}
	}

	@Override
	public Void visitBlock(Block value, Scope scope) {
		for (Statement statement : value.statements) {
			statement.visit(this, scope);
		}
		return null;
	}

//	class Block:
//		int breaklist;      /* list of jumps out of this loop */
//		int nactvar;        /* # active locals outside the breakable structure */
//		boolean upval;      /* true if some variable in the block is an upvalue */
//		boolean isbreakable;/* true if `block' is a loop */

	@Override
	public Void visitFunctionCall(FunctionCall value, Scope scope) {
		// Load args
		int before = scope.top;
		value.args.visit(this, scope);
		int count = scope.top - before;
		// Load function
		value.target.visit(this, scope);
		scope.w.write2(OpCode.CALL, count);
//		scope.top = top + scope.results;
//		throw new IllegalStateException("NYI");
		return null;
	}

//	private void move(String name, Scope scope) {
//		int index = scope.registerLocal(name);
//		scope.w.write2(OpCode.MOV, scope.top - 1, index);
//		if (scope.top - 1 != index) {
//			scope.top--;
//			scope.w.write1(OpCode.POP);
//		}
//	}

//	@Override
//	public Void visitVariable(Variable variable, Scope scope) {
//		move(variable.name, scope);
//		return null;
//	}

	private void stackPop(Scope scope) {
		scope.w.write1(OpCode.POP);
		scope.top--;
	}

	private void stackNil(Scope scope) {
		scope.w.write1(OpCode.CONST_NIL);
		scope.top++;
	}

	@Override
	public Void visitAssignment(Assignment value, Scope scope) {
		if (value.lhs == null) {
			value.rhs.visit(this, scope);
		} else {
			// @TODO Jezza - 09 Feb 2018: Assignment flattening
			// @TODO Jezza - 09 Feb 2018: Conflict resolution

			List<Expression> rhs = value.rhs.list;
			List<Expression> lhs = value.lhs.list;

			int leftSize = lhs.size();
			int rightSize = rhs.size();
			int min = Math.min(leftSize, rightSize);
			int i = 0;
			for (; i < min; i++) {
				rhs.get(i).visit(this, scope);
				lhs.get(i).visit(this, scope);
			}
			if (leftSize < rightSize) {
				for (; i < rightSize; i++) {
					rhs.get(i).visit(this, scope);
					stackPop(scope);
				}
			} else if (leftSize > rightSize) {
				for (; i < leftSize; i++) {
					stackNil(scope);
				}
			}
		}
		return null;
	}

	@Override
	public Void visitExpressionList(ExpressionList value, Scope scope) {
		for (Expression expression : value.list)
			expression.visit(this, scope);
		return null;
	}

	@Override
	public Void visitLabel(Label value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitLocalStatement(LocalStatement value, Scope scope) {
//		if (value.lhs == null) {
//			value.rhs.visit(this, scope);
//		} else {
//			List<String> lhs = value.lhs;
//			List<Expression> rhs = value.rhs.list;
//
//			int results = scope.results;
//
//			int ls = lhs.size();
//			int rs = rhs.size();
//			scope.results = ls;
//			// scope.reserve(ls);
//			if (ls > rs) {
//				for (int i = 0; i < ls; i++) {
//					if (i >= rs) {
//						scope.w.write1(OpCode.CONST_NIL);
//						scope.top++;
//					} else {
//						rhs.get(i).visit(this, scope);
//					}
//					move(lhs.get(i), scope);
//				}
//			} else {
//				for (int i = 0; i < ls; i++) {
//					rhs.get(i).visit(this, scope);
//					move(lhs.get(i), scope);
//				}
//			}
//			scope.results = results;
//		}
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitFunctionBody(FunctionBody value, Scope scope) {
		Scope local = scope.newScope(scope.name + ":function");
		if (value.varargs) {
			throw new IllegalStateException("NYI");
		}
		for (String s : value.parameters) {
			local.registerLocal(s);
		}
		value.body.visit(this, local);

		LuaChunk chunk = local.build();
//		chunk.paramCount = value.parameters.size();
		int index = scope.pool.add(chunk);
		scope.w.write2(OpCode.CONST, index);
		scope.top++;
		return null;
	}

	@Override
	public Void visitGoto(Goto value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitBreak(Break value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitDoBlock(DoBlock value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitWhileLoop(WhileLoop value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitRepeatBlock(RepeatBlock value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitIfBlock(IfBlock value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitForLoop(ForLoop value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitForList(ForList value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitReturnStatement(ReturnStatement value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitUnaryOp(UnaryOp value, Scope scope) {
		value.arg.visit(this, scope);
		scope.w.write1(unaryOpCode(value.op));
		return null;
	}

	private static int unaryOpCode(int code) {
		switch (code) {
			case UnaryOp.OP_MINUS:
				return OpCode.NEG;
			case UnaryOp.OP_NOT:
				return OpCode.NOT;
			case UnaryOp.OP_LEN:
				return OpCode.LEN;
			default:
				throw new IllegalStateException("Unsupported: " + code);
		}
	}

	@Override
	public Void visitBinaryOp(BinaryOp value, Scope scope) {
		if (value.is(ParseTree.FLAG_ASSIGNMENT)) {
			if (value.op != BinaryOp.OP_INDEXED) {
				throw new IllegalStateException("Illegal binary assignment op: " + value.op);
			}
			value.right.visit(this, scope);
			value.left.visit(this, scope);
			scope.w.write1(OpCode.SET_TABLE);
		} else {
			value.left.visit(this, scope);
			value.right.visit(this, scope);
			scope.w.write1(binaryOpCode(value.op));
		}
		return null;
	}

	private static int binaryOpCode(int code) {
		switch (code) {
			case BinaryOp.OP_ADD:
				return OpCode.ADD;
			case BinaryOp.OP_SUB:
				return OpCode.SUB;
			case BinaryOp.OP_MUL:
				return OpCode.MUL;
			case BinaryOp.OP_DIV:
				return OpCode.DIV;
			case BinaryOp.OP_INDEXED:
				return OpCode.DIV;
			default:
				throw new IllegalStateException("Unsupported: " + code);
		}
	}

	@Override
	public Void visitLiteral(Literal value, Scope scope) {
		if (value.is(ParseTree.FLAG_ASSIGNMENT)) {
			if (value.type != Literal.NAMESPACE) {
				throw new IllegalStateException("Attempted to load a non-namespace literal: " + value);
			}
			int index = scope.registerLocal((String) value.value);
			if (scope.top == 0) {
				if (index != 0) {
					throw new IllegalStateException("Illegal stack state?");
				}
			} else if (scope.top >= 0 && scope.top - 1 != index) {
				scope.w.write2(OpCode.MOV, scope.top - 1, index);
				stackPop(scope);
			}
		} else if (value.type == Literal.NAMESPACE) {
			int to = scope.top;
			int from = scope.locals.add(value.value);
			if (from != to) {
				stackNil(scope);
				scope.w.write2(OpCode.MOV, from, to);
			}
		} else {
			int index = scope.pool.add(value.value);
			scope.w.write2(OpCode.CONST, index);
			scope.top++;
		}

//		ByteCodeWriter w = scope.w;
//		if (value.type == Literal.NAMESPACE) {
//			String name = (String) value.value;
////			Scope current = scope;
////			while (current != null) {
////				int index = current.locals.add(name);
////				if (index != -1) {
////					// TODO: 18/06/2017 Upvalues should be added here.
////					if (current != scope)
////						throw new IllegalStateException("Upvalues aren't supported: " + name);
////					w.write1(OpCodes.CONST1, index);
////					scope.top++;
////					return null;
////				}
////				current = current.previous;
////			}
//			int index = scope.pool.add(name);
//			w.write1(OpCode.CONST1, index);
//			w.write1(OpCode.GET_GLOBAL);
//			scope.top++;
//		} else {
//			scope.top++;
//		}
		return null;
	}

	@Override
	public Void visitVarargs(Varargs value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitTableConstructor(TableConstructor value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitTableField(TableField value, Scope scope) {
		throw new IllegalStateException("NYI");
	}
}
