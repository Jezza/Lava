package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.FLAG_ASSIGNMENT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_GLOBAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_UPVAL;

import java.util.ArrayList;
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
import me.jezza.lava.lang.interfaces.Visitor;
import me.jezza.lava.lang.util.ByteCodeWriter;
import me.jezza.lava.lang.util.ConstantPool;
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
		final ConstantPool<Object> pool;
		final List<Name> upvalues;

		private int index;
		private int max;

		Scope(String name) {
			this(name, null);
		}

		private Scope(String name, Scope previous) {
			this.name = name;
			this.previous = previous;
			w = new ByteCodeWriter();
			pool = new ConstantPool<>();
			upvalues = new ArrayList<>();
//			allocator = new AllocationList();
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
			chunk.maxStackSize = max;
			chunk.upvalues = upvalues.toArray(new Name[0]);
			return chunk;
		}

		public int upvalue(Name name) {
			for (int i = 0, size = upvalues.size(); i < size; i++) {
				Name other = upvalues.get(i);
				if (other.value.equals(name.value)) {
					return i;
				}
			}
			int value = upvalues.size();
			upvalues.add(name);
			return value;
		}
		
		public int mark() {
			return index;
		}

		public int allocate() {
			if (index == max) {
				max = index + 1;
			}
			return index++;
		}

		public int allocate(int count) {
			int in = index;
			index += count;
			if (index > max) {
				max = index;
			}
			return in;
		}
		
		public int pop() {
			return --index;
		}

		public int pop(int count) {
			index -= count;
			return index;
		}

//		public void allocate(Address desc) {
//		}

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

	static final class Address {
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
	}

	@Override
	public Object visitBlock(Block value, Scope scope) {
		scope.allocate(value.names.size());
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
		int base = scope.mark();
		// Load function
		value.target.visit(this, scope);
		// Load args
		List<Expression> args = value.args.list;
		int count = args.size();
		for (int i = 0; i < count; i++) {
			args.get(i).visit(this, scope);
		}
		scope.w.write2(OpCode.CALL, base, count, value.expectedResults);
		scope.pop(count + 1);
		scope.allocate(value.expectedResults);
		return null;
	}

	@Override
	public Object visitAssignment(Assignment value, Scope scope) {
		if (value.lhs == null) {
			value.rhs.visit(this, scope);
		} else {
			// @TODO Jezza - 09 Feb 2018: Assignment flattening
			// @TODO Jezza - 09 Feb 2018: Conflict resolution
			value.rhs.visit(this, scope);
			List<Expression> lhs = value.lhs.list;
			for (int i = lhs.size() - 1; i >= 0; i--) {
				lhs.get(i).visit(this, scope);
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionList(ExpressionList value, Scope scope) {
		for (Expression expression : value.list) {
			expression.visit(this, scope);
		}
		return null;
	}

	@Override
	public Object visitFunctionBody(FunctionBody value, Scope scope) {
		if (value.varargs) {
			throw new IllegalStateException("NYI");
		}
		Scope local = scope.newScope(scope.name + ":function");
		value.body.visit(this, local);

		LuaChunk chunk = local.build();
//		chunk.params = value.parameters.size();
		int index = scope.pool.add(chunk);
		int register = scope.allocate();
		scope.w.write2(OpCode.LOAD_FUNC, index, register);
		return null;
	}

	@Override
	public Object visitLabel(Label value, Scope scope) {
		throw new IllegalStateException("NYI");
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
		Scope local = scope.newScope(scope.name + ":do_end");
		value.body.visit(this, local);

		LuaChunk chunk = local.build();
		int index = scope.pool.add(chunk);
		int register = scope.mark();
		// @CLEANUP Jezza - 28 Feb 2018: Not the best solution...
		scope.w.write2(OpCode.CONST, index, register);
		scope.w.write2(OpCode.CALL, register, 0, 0);
		return null;
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
		ExpressionList expressions = value.expressions;
		if (expressions.size() != 0) {
			int position = scope.mark();
			expressions.visit(this, scope);
			scope.w.write1(OpCode.RETURN, expressions.size(), position);
		} else {
			scope.w.write1(OpCode.RETURN, 0, 0);
		}
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
			value.left.visit(this, scope);
			value.right.visit(this, scope);
			int key = scope.pop();
			int table = scope.pop();
			int register = scope.pop();
			
			scope.w.write2(OpCode.SET_TABLE, table, key, register);
		} else {
			value.left.visit(this, scope);
			value.right.visit(this, scope);

			int right = scope.pop();
			int left = scope.pop();
			int result = scope.allocate();

			// @TODO Jezza - 11 Feb 2018: Constant folding..
			// Should I do that here or in the parser?

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
				return OpCode.GET_TABLE;
			default:
				throw new IllegalStateException("Unsupported: " + code);
		}
	}

	private void loadConstant(Object value, Scope scope) {
		int constant = scope.pool.add(value);
		int register = scope.allocate();
		scope.w.write2(OpCode.CONST, constant, register);
	}

	@Override
	public Object visitLiteral(Literal value, Scope scope) {
		switch (value.type) {
			case Literal.STRING:
			case Literal.DOUBLE:
			case Literal.INTEGER: {
				loadConstant(value.value, scope);
				break;
			}
			case Literal.FALSE: {
				int register = scope.allocate();
				scope.w.write2(OpCode.CONST_FALSE, register);
				break;
			}
			case Literal.TRUE: {
				int register = scope.allocate();
				scope.w.write2(OpCode.CONST_TRUE, register);
				break;
			}
			case Literal.NIL: {
				int register = scope.allocate();
				scope.w.write2(OpCode.CONST_NIL, register);
				break;
			}
			default:
				throw new IllegalStateException("Unknown literal type: " + value.type);
		}
		return null;
	}

	@Override
	public Object visitName(Name value, Scope scope) {
		if (value.is(FLAG_ASSIGNMENT)) {
			assignName(value, scope);
		} else {
			loadName(value, scope);
		}
		return null;
	}

	private void assignName(Name value, Scope scope) {
		if (value.is(FLAG_LOCAL)) {
			int register = scope.pop();
			scope.w.write2(OpCode.MOVE, register, value.index);
		} else if (value.is(FLAG_GLOBAL)) {
			loadConstant(value.value, scope);
			int constant = scope.pop();
			int register = scope.pop();
			scope.w.write2(OpCode.SET_GLOBAL, constant, register);
		} else if (value.is(FLAG_UPVAL)) {
			int register = scope.pop();
			int index = scope.upvalue(value);
			scope.w.write2(OpCode.SET_UPVAL, register, index);
		} else {
			throw new IllegalStateException("Invalid name state: " + value);
		}
	}

	private void loadName(Name value, Scope scope) {
		if (value.is(FLAG_LOCAL)) {
			int register = scope.allocate();
			scope.w.write2(OpCode.MOVE, value.index, register);
		} else if (value.is(FLAG_GLOBAL)) {
			loadConstant(value.value, scope);
			int constant = scope.pop();
			int register = scope.allocate();
			scope.w.write2(OpCode.GET_GLOBAL, constant, register);
		} else if (value.is(FLAG_UPVAL)) {
			int index = scope.upvalue(value);
			int register = scope.allocate();
			scope.w.write2(OpCode.GET_UPVAL, index, register);
		} else {
			throw new IllegalStateException("Invalid name state: " + value);
		}
	}

	@Override
	public Object visitVarargs(Varargs value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitTableConstructor(TableConstructor value, Scope scope) {
		int register = scope.allocate();
		scope.w.write2(OpCode.NEW_TABLE, register);
		for (TableField field : value.fields) {
			field.visit(this, scope);
		}
		return null;
	}

	@Override
	public Object visitTableField(TableField value, Scope scope) {
		value.key.visit(this, scope);
		value.value.visit(this, scope);
		int val = scope.pop();
		int key = scope.pop();
		// We don't want to pop the value, because there might be more fields.
		int table = scope.mark() - 1;
		scope.w.write2(OpCode.SET_TABLE, table, key, val);
		return null;
	}
}
