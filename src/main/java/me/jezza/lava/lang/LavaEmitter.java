package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.FLAG_ASSIGNMENT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_GLOBAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_UPVAL;

import java.util.ArrayList;
import java.util.List;

import me.jezza.lava.lang.LavaEmitter.Context;
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
public final class LavaEmitter implements Visitor<Context, Object> {

	public static LuaChunk emit(String name, FunctionBody node) {
		LavaEmitter emitter = new LavaEmitter();
		Context context = new Context(name);
		node.body.visit(emitter, context);
		LuaChunk build = context.build();
		build.parameterCount = node.parameters.size();
		build.varargs = node.varargs;
		return build;
	}

	static final class Context {
		final String name;
		final Context previous;
		final ByteCodeWriter w;
		final ConstantPool<Object> pool;
		final List<Name> upvalues;

		private int index;
		private int max;

		Context(String name) {
			this(name, null);
		}

		private Context(String name, Context previous) {
			this.name = name;
			this.previous = previous;
			w = new ByteCodeWriter();
			pool = new ConstantPool<>();
			upvalues = new ArrayList<>();
		}

		Context newContext(String name) {
			return new Context(name, this);
		}

		LuaChunk build() {
			LuaChunk chunk = new LuaChunk(name);
			chunk.constants = pool.build();
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
	}

	@Override
	public Object visitBlock(Block value, Context context) {
		context.allocate(value.names.size());
		for (Statement statement : value.statements) {
			statement.visit(this, context);
		}
		context.w.write2(OpCode.CLOSE_SCOPE, value.offset);
		return null;
	}

	@Override
	public Object visitFunctionCall(FunctionCall value, Context context) {
		int base = context.mark();
		// Load function
		value.target.visit(this, context);
		// Load args
		List<Expression> args = value.args.list;
		int count = args.size();
		for (int i = 0; i < count; i++) {
			args.get(i).visit(this, context);
		}
		int expected = value.expectedResults;
		context.w.write2(OpCode.CALL, base, count, expected);
		// Pop the arguments and the function
		context.pop(count + 1);
		// Allocate the expected results.

		// Allocate at least one result,
		// because if this is inside a
		// function call itself, it tries to deallocate it.)
		context.allocate(expected > 0
				? expected
				: 1);
		return null;
	}

	@Override
	public Object visitAssignment(Assignment value, Context context) {
		if (value.lhs == null) {
			value.rhs.visit(this, context);
		} else {
			// @TODO Jezza - 09 Feb 2018: Assignment flattening
			// @TODO Jezza - 09 Feb 2018: Conflict resolution
			value.rhs.visit(this, context);
			List<Expression> lhs = value.lhs.list;
			for (int i = lhs.size() - 1; i >= 0; i--) {
				lhs.get(i).visit(this, context);
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionList(ExpressionList value, Context context) {
		for (Expression expression : value.list) {
			expression.visit(this, context);
		}
		return null;
	}

	@Override
	public Object visitFunctionBody(FunctionBody value, Context context) {
		Context local = context.newContext(context.name + ":function");
		value.body.visit(this, local);

		LuaChunk chunk = local.build();
		chunk.parameterCount = value.parameters.size();
		chunk.varargs = value.varargs;
		int index = context.pool.add(chunk);
		int register = context.allocate();
		context.w.write2(OpCode.LOAD_FUNC, index, register);
		return null;
	}

	@Override
	public Object visitLabel(Label value, Context context) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitGoto(Goto value, Context context) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitBreak(Break value, Context context) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitDoBlock(DoBlock value, Context context) {
		value.body.visit(this, context);
		return null;
	}

	@Override
	public Object visitRepeatBlock(RepeatBlock value, Context context) {
		int start = context.w.mark();
		value.body.visit(this, context);
		value.condition.visit(this, context);
		int register = context.pop();
		context.w.write2(OpCode.IF_FALSE, register, start);
		return null;
	}

	@Override
	public Object visitIfBlock(IfBlock value, Context context) {
		value.condition.visit(this, context);
		int register = context.pop();
		context.w.write2(OpCode.IF_FALSE, register);
		int elseJump = context.w.mark2();
		value.thenPart.visit(this, context);
		context.w.write1(OpCode.GOTO);
		int thenJump = context.w.mark2();
		context.w.backPatch2(elseJump);
		if (value.elsePart != null) {
			value.elsePart.visit(this, context);
		}
		context.w.backPatch2(thenJump);
		return null;
	}

	@Override
	public Object visitForLoop(ForLoop value, Context context) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitForList(ForList value, Context context) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Object visitReturnStatement(ReturnStatement value, Context context) {
		ExpressionList expressions = value.expressions;
		if (expressions.size() != 0) {
			int position = context.mark();
			expressions.visit(this, context);
			context.w.write1(OpCode.RETURN, expressions.size(), position);
		} else {
			context.w.write1(OpCode.RETURN, 0, 0);
		}
		return null;
	}

	@Override
	public Object visitUnaryOp(UnaryOp value, Context context) {
		value.arg.visit(this, context);
		int result = context.pop();
		int register = context.allocate();
		context.w.write2(unaryOpCode(value.op), result, register);
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
	public Object visitBinaryOp(BinaryOp value, Context context) {
		if (value.is(FLAG_ASSIGNMENT)) {
			if (value.op != BinaryOp.OP_INDEXED) {
				throw new IllegalStateException("Illegal binary assignment op: " + value.op);
			}
			value.left.visit(this, context);
			value.right.visit(this, context);
			int key = context.pop();
			int table = context.pop();
			int register = context.pop();

			context.w.write2(OpCode.SET_TABLE, table, key, register);
		} else {
			value.left.visit(this, context);
			value.right.visit(this, context);

			int right = context.pop();
			int left = context.pop();
			int result = context.allocate();

			// @TODO Jezza - 11 Feb 2018: Constant folding..
			// Should I do that here or in the parser?

			int op = binaryOpCode(value.op);
			context.w.write2(op, result, left, right);
		}
		return null;
	}

	private static int binaryOpCode(int code) {
		switch (code) {
			case BinaryOp.OP_EQ:
				return OpCode.EQ;
			case BinaryOp.OP_ADD:
				return OpCode.ADD;
			case BinaryOp.OP_SUB:
				return OpCode.SUB;
			case BinaryOp.OP_MUL:
				return OpCode.MUL;
			case BinaryOp.OP_DIV:
				return OpCode.DIV;
			case BinaryOp.OP_LT:
				return OpCode.LT;
			case BinaryOp.OP_INDEXED:
				return OpCode.GET_TABLE;
			default:
				throw new IllegalStateException("Unsupported: " + code);
		}
	}

	private void loadConstant(Object value, Context context) {
		int constant = context.pool.add(value);
		int register = context.allocate();
		context.w.write2(OpCode.CONST, constant, register);
	}

	@Override
	public Object visitLiteral(Literal value, Context context) {
		switch (value.type) {
			case Literal.STRING:
			case Literal.DOUBLE:
			case Literal.INTEGER: {
				loadConstant(value.value, context);
				break;
			}
			case Literal.FALSE: {
				int register = context.allocate();
				context.w.write2(OpCode.CONST_FALSE, register);
				break;
			}
			case Literal.TRUE: {
				int register = context.allocate();
				context.w.write2(OpCode.CONST_TRUE, register);
				break;
			}
			case Literal.NIL: {
				int register = context.allocate();
				context.w.write2(OpCode.CONST_NIL, register);
				break;
			}
			default:
				throw new IllegalStateException("Unknown literal type: " + value.type);
		}
		return null;
	}

	@Override
	public Object visitName(Name value, Context context) {
		if (value.is(FLAG_ASSIGNMENT)) {
			writeName(value, context);
		} else {
			readName(value, context);
		}
		return null;
	}

	private void writeName(Name value, Context context) {
		if (value.is(FLAG_LOCAL)) {
			int register = context.pop();
			context.w.write2(OpCode.MOVE, register, value.index);
		} else if (value.is(FLAG_GLOBAL)) {
			loadConstant(value.value, context);
			int constant = context.pop();
			int register = context.pop();
			context.w.write2(OpCode.SET_GLOBAL, constant, register);
		} else if (value.is(FLAG_UPVAL)) {
			int register = context.pop();
			int index = context.upvalue(value);
			context.w.write2(OpCode.SET_UPVAL, register, index);
		} else {
			throw new IllegalStateException("Invalid name state: " + value);
		}
	}

	private void readName(Name value, Context context) {
		if (value.is(FLAG_LOCAL)) {
			int register = context.allocate();
			context.w.write2(OpCode.MOVE, value.index, register);
		} else if (value.is(FLAG_GLOBAL)) {
			loadConstant(value.value, context);
			int constant = context.pop();
			int register = context.allocate();
			context.w.write2(OpCode.GET_GLOBAL, constant, register);
		} else if (value.is(FLAG_UPVAL)) {
			int index = context.upvalue(value);
			int register = context.allocate();
			context.w.write2(OpCode.GET_UPVAL, index, register);
		} else {
			throw new IllegalStateException("Invalid name state: " + value);
		}
	}

	@Override
	public Object visitVarargs(Varargs value, Context context) {
		if (value.expectedResults == Varargs.UNBOUNDED) {
			int target = context.mark();
			context.w.write2(OpCode.VARARGS, -1, target);
			return null;
		}
		for (int i = 0, l = value.expectedResults; i < l; i++) {
			int target = context.allocate();
			context.w.write2(OpCode.VARARGS, i, target);
		}
		return null;
	}

	@Override
	public Object visitTableConstructor(TableConstructor value, Context context) {
		int register = context.allocate();
		context.w.write2(OpCode.NEW_TABLE, register);
		for (TableField field : value.fields) {
			field.visit(this, context);
		}
		return null;
	}

	@Override
	public Object visitTableField(TableField value, Context context) {
		value.key.visit(this, context);
		value.value.visit(this, context);
		int val = context.pop();
		int key = context.pop();
		// We don't want to pop the value, because there might be more fields.
		int table = context.mark() - 1;
		context.w.write2(OpCode.SET_TABLE, table, key, val);
		return null;
	}
}
