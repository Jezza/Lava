package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.FLAG_ASSIGNMENT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;

import java.util.List;

import me.jezza.lava.lang.LavaEmitter.Scope;
import me.jezza.lava.lang.ParseTree.Assignment;
import me.jezza.lava.lang.ParseTree.BinaryOp;
import me.jezza.lava.lang.ParseTree.Block;
import me.jezza.lava.lang.ParseTree.Break;
import me.jezza.lava.lang.ParseTree.DoBlock;
import me.jezza.lava.lang.ParseTree.Expression;
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
import me.jezza.lava.lang.ParseTree.WhileLoop;
import me.jezza.lava.lang.interfaces.Visitor;
import me.jezza.lava.runtime.Interpreter.LuaChunk;
import me.jezza.lava.runtime.OpCode;

/**
 * @author Jezza
 */
public final class LavaEmitter implements Visitor<Scope, Object> {

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
//		final AllocationList locals;

//		int top;
//		int max;
//		ExpDesc active;

		Scope(String name) {
			this(name, null);
		}

		private Scope(String name, Scope previous) {
			this.name = name;
			this.previous = previous;
			this.w = new ByteCodeWriter();
			this.pool = new ConstantPool();
//			locals = new AllocationList();
		}

		Scope newScope(String name) {
			return new Scope(name, this);
		}

		LuaChunk build() {
			LuaChunk chunk = new LuaChunk(name);
			chunk.constants = pool.build();
			if (w.mark() == 0 || w.get(w.mark() - 1) != OpCode.RETURN) {
				w.write2(OpCode.RETURN, 0);
			}
			chunk.code = w.code();
			chunk.maxStackSize = index + 1;
			return chunk;
		}
		
		
		private int[] indices = new int[2048];
		private int index;
		private int max;
		
		// @TODO Jezza - 19 Feb 2018: 
		private boolean[] registers = new boolean[2048];
		
		public int allocate() {
			for (int i = 0, l = registers.length; i < l; i++) {
				if (!registers[i]) {
					registers[i] = true;
					indices[index] = i;
					if (index > max) {
						max = index;
					}
					index++;
					return i;
				}
			}
			throw new IllegalStateException("Allocation full");
		}
		
		public int pop() {
			this.index--;
			int index = indices[this.index];
			indices[this.index] = 0;
			registers[index] = false;
			return index;
		}

//		ExpDesc find(Object value) {
//			int poolIndex = locals.indexOf(value);
//			if (poolIndex >= 0) {
//				return new ExpDesc(ExpDesc.LOCAL, poolIndex);
//			}
//			if (previous != null) {
//				ExpDesc desc = previous.find(value);
//				if (desc.type == ExpDesc.GLOBAL) {
//					return desc;
//				}
//				} else if (op.type == ExpOp.LOCAL) {
//					op.type = ExpOp.UPVAL;
//				throw new IllegalStateException("NYI (UPVAL)");
//			}
//			poolIndex = pool.add(value);
//			return new ExpDesc(ExpDesc.GLOBAL, poolIndex);
//		}

//		int registerLocal(String name) {
//			return locals.register(name);
//		}
	}

//	static final class ExpDesc {
//		static final int VOID = 0;
//		static final int CONSTANT = 1;
//
//		static final int CONSTANT_NIL = 2;
//		static final int CONSTANT_TRUE = 3;
//		static final int CONSTANT_FALSE = 4;
//		static final int CONSTANT_INTEGER = 5;
//		static final int CONSTANT_DOUBLE = 6;
//
//		static final int LOCAL = 6;
//		static final int UPVAL = 7;
//		static final int GLOBAL = 8;
//
//		static final int BINARY = 9;
//		static final int RELOCATABLE0 = 10;
//		static final int CALL = 10;
//
//		int type;
//		int target;
//
//		int left;
//		int right;
//
//		Object payload;
//
//		ExpDesc() {
//			this(VOID);
//		}
//
//		ExpDesc(int type) {
//			this.type = type;
//		}
//
//		ExpDesc(int type, int target) {
//			this.type = type;
//			this.target = target;
//		}
//
//		ExpDesc(int type, Object payload) {
//			this.type = type;
//			this.payload = payload;
//		}
//
//		int emit(Scope scope) {
//			int index = scope.locals.allocate();
//			emit(scope, index);
//			return index;
//		}
//
//		int emitThenFree(Scope scope) {
//			int index = scope.locals.allocate();
//			emit(scope, index);
//			scope.locals.free(index);
//			return index;
//		}
//		
//		void emit(Scope scope, int target) {
////			if (target > max) {
////				max = target;
////			}
//			switch (type) {
//				case CONSTANT_INTEGER:
//				case CONSTANT_DOUBLE:
//				case CONSTANT: {
//					this.target = scope.pool.add(payload);
//					scope.w.write2(OpCode.CONST, this.target, target);
//					break;
//				}
//				case ExpDesc.CONSTANT_NIL: {
//					scope.w.write2(OpCode.CONST_NIL, target);
//					break;
//				}
//				case ExpDesc.CONSTANT_TRUE: {
//					scope.w.write2(OpCode.CONST_TRUE, target);
//					break;
//				}
//				case ExpDesc.CONSTANT_FALSE: {
//					scope.w.write2(OpCode.CONST_FALSE, target);
//					break;
//				}
//				case ExpDesc.CALL: {
//					int base = this.target;
//					int paramCount = left;
//					int expected = right;
//					scope.w.write2(OpCode.CALL, base, paramCount, expected);
//					if (--right > 0) {
//						return;
//					}
//					break;
//				}
//				case ExpDesc.BINARY: {
//					int op = this.target;
//					int leftSlot = left;
//					int rightSlot = right;
//					scope.w.write2(op, leftSlot, rightSlot, target);
//					break;
//				}
//				case ExpDesc.VOID:
//					break;
//				default:
//					throw new IllegalStateException("Unsupported ExpOp type: " + type);
//			}
//			type = ExpDesc.VOID;
//			this.target = -1;
//			payload = null;
//			scope.active = null;
//		}
//	}

	@Override
	public Object visitBlock(Block value, Scope scope) {
		for (int i = 0, l = value.names.size(); i < l; i++) {
			scope.allocate();
		}
		
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
	public Object visitFunctionCall(FunctionCall value, Scope scope) {
		// Load args
		List<Expression> args = value.args.list;
		int count = args.size();
		int index = scope.allocate() - 1;
		scope.pop();
		for (int i = 0; i < count; i++) {
			args.get(i).visit(this, scope);
		}
		// Load function
		value.target.visit(this, scope);
		scope.w.write2(OpCode.CALL, index, count, value.expectedResults);
		return null;
	}

//	private ExpOp move(String name, Scope scope) {
//		int index = scope.registerLocal(name);
//		scope.w.write2(OpCode.MOV, scope.top - 1, index);
//		if (scope.top - 1 != index) {
//			scope.top--;
//			scope.w.write1(OpCode.POP);
//		}
//	}

//	@Override
//	public ExpOp visitVariable(Variable variable, Scope scope) {
//		move(variable.name, scope);
//		return null;
//	}

	@Override
	public Object visitAssignment(Assignment value, Scope scope) {
		if (value.lhs == null) {
			value.rhs.visit(this, scope);
			scope.pop();
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
//			if (leftSize < rightSize) {
//				throw new IllegalStateException("NYI (assignment buffering)");
//				for (; i < rightSize; i++) {
//					rhs.get(i).visit(this, scope);
//					stackPop(scope);
//				}
//			} else if (leftSize > rightSize) {
//				throw new IllegalStateException("Parser should have already handled this.");
//				for (; i < leftSize; i++) {
//					stackNil(scope);
//				}
//			}
		}
		return null;
	}

	@Override
	public Object visitExpressionList(ExpressionList value, Scope scope) {
		for (Expression expression : value.list)
			expression.visit(this, scope);
		return null;
	}

	@Override
	public Object visitLabel(Label value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

//	@Override
//	public ExpDesc visitLocalStatement(LocalStatement value, Scope scope) {
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
//		throw new IllegalStateException("NYI");
//	}

	@Override
	public Object visitFunctionBody(FunctionBody value, Scope scope) {
		if (value.varargs) {
			throw new IllegalStateException("NYI");
		}
		Scope local = scope.newScope(scope.name + ":function");
		value.body.visit(this, local);

		LuaChunk chunk = local.build();
		chunk.paramCount = value.parameters.size();
		int index  = scope.pool.add(chunk);
		int register = scope.allocate();
		scope.w.write2(OpCode.CONST, index, register);
		return null;
	}

	@Override
	public Object visitGoto(Goto value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitBreak(Break value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitDoBlock(DoBlock value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitWhileLoop(WhileLoop value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitRepeatBlock(RepeatBlock value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitIfBlock(IfBlock value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitForLoop(ForLoop value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitForList(ForList value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitReturnStatement(ReturnStatement value, Scope scope) {
		ExpressionList exprs = value.exprs;
		exprs.visit(this, scope);
		int count = exprs.size();
		scope.w.write1(OpCode.RETURN, count);
		return null;
	}

	@Override
	public Object visitUnaryOp(UnaryOp value, Scope scope) {
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
	public Object visitBinaryOp(BinaryOp value, Scope scope) {
		if (value.is(FLAG_ASSIGNMENT)) {
			if (value.op != BinaryOp.OP_INDEXED) {
				throw new IllegalStateException("Illegal binary assignment op: " + value.op);
			}
//			value.right.visit(this, scope);
//			value.left.visit(this, scope);
//			scope.w.write1(OpCode.SET_TABLE);
			throw new IllegalStateException("NYI (set_table)");
		} else {
			value.left.visit(this, scope);
			value.right.visit(this, scope);

			int right = scope.pop();
			int left = scope.pop();
			int result = scope.allocate();
//
//			// @TODO Jezza - 11 Feb 2018: Constant folding..
//			// Should I do that here or in the parser?

			int op = binaryOpCode(value.op);
			scope.w.write2(op, result, left, right);
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
	public Object visitLiteral(Literal value, Scope scope) {
		switch (value.type) {
			case Literal.STRING:
			case Literal.DOUBLE:
			case Literal.INTEGER: {
				int constant = scope.pool.add(value.value);
				int register = scope.allocate();
				scope.w.write2(OpCode.CONST, constant, register);
				break;
			}
			case Literal.FALSE: {
				int register = scope.allocate();
				scope.w.write2(OpCode.CONST_FALSE, register);
				break;
			}
			case Literal.TRUE: {
				int allocate = scope.allocate();
				scope.w.write2(OpCode.CONST_TRUE, allocate);
				break;
			}
			case Literal.NIL: {
				int allocate = scope.allocate();
				scope.w.write2(OpCode.CONST_NIL, allocate);
				break;
			}
			default:
				throw new IllegalStateException("Unknown literal type: " + value.type);
		}
		return null;
	}

//		int type = descriptorType(value.type);
//		return new ExpDesc(type, value.value);

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

//	private static int descriptorType(int literalType) {
//		switch (literalType) {
//			case Literal.NIL:
//				return ExpDesc.CONSTANT_NIL;
//			case Literal.TRUE:
//				return ExpDesc.CONSTANT_TRUE;
//			case Literal.FALSE:
//				return ExpDesc.CONSTANT_FALSE;
//			case Literal.STRING:
//				return ExpDesc.CONSTANT;
//			case Literal.INTEGER:
//				return ExpDesc.CONSTANT_INTEGER;
//			case Literal.DOUBLE:
//				return ExpDesc.CONSTANT_DOUBLE;
//			default:
//				throw new IllegalStateException("Unsupported literal type conversion.");
//		}
//	}

	@Override
	public Object visitName(Name value, Scope scope) {
		boolean local = value.is(FLAG_LOCAL);
		boolean assign = value.is(FLAG_ASSIGNMENT);
		if (local && assign) {
			int register = scope.pop();
			scope.w.write2(OpCode.MOVE, register, value.index);
			return null;
		} else if (local) {
			int register = scope.allocate();
			scope.w.write2(OpCode.MOVE, value.index, register);
			return null;
		} else if (assign) {
			if (value.index == -1) {
				throw new IllegalStateException("set_global not yet supported");
			}
			throw new IllegalStateException("upvalue assign not yet supported");
		}
		throw new IllegalStateException("get_global not yet supported");
	}

	@Override
	public Object visitVarargs(Varargs value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitTableConstructor(TableConstructor value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitTableField(TableField value, Scope scope) {
		throw new IllegalStateException("NYI");
	}
}
