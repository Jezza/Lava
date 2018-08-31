package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.Block.FLAG_CUSTOM_SCOPE;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_GLOBAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_UPVAL;
import static me.jezza.lava.runtime.OpCode.ADD;
import static me.jezza.lava.runtime.OpCode.CALL;
import static me.jezza.lava.runtime.OpCode.CLOSE_SCOPE;
import static me.jezza.lava.runtime.OpCode.CONCAT;
import static me.jezza.lava.runtime.OpCode.CONST;
import static me.jezza.lava.runtime.OpCode.CONST_FALSE;
import static me.jezza.lava.runtime.OpCode.CONST_NIL;
import static me.jezza.lava.runtime.OpCode.CONST_TRUE;
import static me.jezza.lava.runtime.OpCode.DIV;
import static me.jezza.lava.runtime.OpCode.EQ;
import static me.jezza.lava.runtime.OpCode.GET_GLOBAL;
import static me.jezza.lava.runtime.OpCode.GET_TABLE;
import static me.jezza.lava.runtime.OpCode.GET_UPVAL;
import static me.jezza.lava.runtime.OpCode.GOTO;
import static me.jezza.lava.runtime.OpCode.LE;
import static me.jezza.lava.runtime.OpCode.LEN;
import static me.jezza.lava.runtime.OpCode.LOAD_FUNC;
import static me.jezza.lava.runtime.OpCode.LT;
import static me.jezza.lava.runtime.OpCode.MOD;
import static me.jezza.lava.runtime.OpCode.MOVE;
import static me.jezza.lava.runtime.OpCode.MUL;
import static me.jezza.lava.runtime.OpCode.NEG;
import static me.jezza.lava.runtime.OpCode.NEW_TABLE;
import static me.jezza.lava.runtime.OpCode.NOT;
import static me.jezza.lava.runtime.OpCode.POW;
import static me.jezza.lava.runtime.OpCode.RETURN;
import static me.jezza.lava.runtime.OpCode.SET_GLOBAL;
import static me.jezza.lava.runtime.OpCode.SET_TABLE;
import static me.jezza.lava.runtime.OpCode.SET_UPVAL;
import static me.jezza.lava.runtime.OpCode.SUB;
import static me.jezza.lava.runtime.OpCode.TEST;
import static me.jezza.lava.runtime.OpCode.TO_NUMBER;
import static me.jezza.lava.runtime.OpCode.TO_STRING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.jezza.lava.lang.LavaEmitter.Context;
import me.jezza.lava.lang.LavaEmitter.Item;
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

/**
 * @author Jezza
 */
public final class LavaEmitter implements Visitor<Context, Item> {

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

		final Map<String, Integer> labels;

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
			upvalues = new ArrayList<>(0);
			labels = new HashMap<>();
		}

		Context newContext(String name) {
			return new Context(this.name + "::" + name, this);
		}

		LuaChunk build() {
			LuaChunk chunk = new LuaChunk(name);
			chunk.constants = pool.build();
			chunk.code = w.code();
			chunk.maxStackSize = max;
			chunk.upvalues = upvalues.toArray(new Name[0]);
			System.out.println("Chunk size: " + chunk.code.length);
			return chunk;
		}

		int loadConstant(Object value) {
			int register = allocate();
			int constant = pool.add(value);
			w.write2(CONST, constant, register);
			return register;
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
	public Item visitBlock(Block value, Context context) {
		context.allocate(value.names.size());
		for (Statement statement : value.statements) {
			statement.visit(this, context);
		}
		if (!value.is(FLAG_CUSTOM_SCOPE)) {
			context.w.write2(CLOSE_SCOPE, value.offset);
		}
		return null;
	}

	@Override
	public Item visitFunctionCall(FunctionCall value, Context context) {
		Item item = value.target.visit(this, context);
		// Load function into working memory
		int base = context.allocate();
		Item loaded = item.load(base);
		// Load args
		List<Expression> args = value.args.list;
		int count = args.size();
		int start = context.allocate(count);
		for (int i = 0; i < count; i++) {
			args.get(i).visit(this, context).load(start + i);
		}
		int expected = value.expectedResults;
		// Invoke the function
		return loaded.invoke(base, count, expected);
	}

	@Override
	public Item visitAssignment(Assignment value, Context context) {
		if (value.lhs == null) {
			value.rhs.visit(this, context).drop();
		} else {
			// @TODO Jezza - 09 Feb 2018: Conflict resolution
			List<Item> left = ((ChainItem) value.lhs.visit(this, context)).items;
			List<Item> right = ((ChainItem) value.rhs.visit(this, context)).items;

			if (left.size() > right.size()) {
				throw new IllegalStateException("Unbalanced assignment sides");
			}

			int size = left.size();
			for (int i = 0; i < size; i++) {
				left.get(i).store(right.get(i));
			}
		}
		return null;
	}

	@Override
	public Item visitExpressionList(ExpressionList value, Context context) {
		List<Item> items = new ArrayList<>(2);
		for (Expression expression : value.list) {
			Item item = expression.visit(this, context);
			if (item instanceof InvokeResultItem) {
				((InvokeResultItem) item).unpack(items);
			} else {
				items.add(item);
			}
		}
		return new ChainItem(context, items);
	}

	@Override
	public Item visitFunctionBody(FunctionBody value, Context context) {
		Context local = context.newContext(value.name);
		value.body.visit(this, local);

		LuaChunk chunk = local.build();
		chunk.parameterCount = value.parameters.size();
		chunk.varargs = value.varargs;
		return new ConstantItem(context, chunk);
	}

	@Override
	public Item visitLabel(Label value, Context context) {
//		context.labels.put(value.name, context.w.mark());
//		return null;
		throw new IllegalStateException("NYI");
	}

	@Override
	public Item visitGoto(Goto value, Context context) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Item visitBreak(Break value, Context context) {
//		context.w.write1(OpCode.GOTO);
//		context.w.addJump2();
//		return null;
		throw new IllegalStateException("NYI");
	}

	@Override
	public Item visitDoBlock(DoBlock value, Context context) {
		value.body.visit(this, context);
		return null;
	}

	@Override
	public Item visitRepeatBlock(RepeatBlock value, Context context) {
		int start = context.w.mark();
		value.body.visit(this, context);
		CondItem cond = value.condition.visit(this, context)
				.cond();
		int exit = cond.jumpTrue();
		context.w.write2(GOTO, start);
		context.w.patchToHere2(exit);
		context.w.write2(CLOSE_SCOPE, value.body.offset);

//		int start = context.w.mark();
//		value.body.visit(this, context).drop();
//		value.condition.visit(this, context);
//				.cond(OpCode.IF_FALSE, start);
//		int register = context.pop();
//		context.w.write2(OpCode.IF_FALSE, register, start);
//		return null;
//		throw new IllegalStateException("NYI");
		return null;
	}

	@Override
	public Item visitIfBlock(IfBlock value, Context context) {
		CondItem cond = value.condition.visit(this, context)
				.cond();
		int target = cond.jumpFalse();
		cond.insertTrueTarget();
		value.thenPart.visit(this, context);
		context.w.write1(GOTO);
		int exit = context.w.reserve2();
		context.w.patchToHere2(target);
		cond.insertFalseTarget();
		if (value.elsePart != null) {
			value.elsePart.visit(this, context);
		}
		context.w.patchToHere2(exit);
		return null;
	}

	@Override
	public Item visitForLoop(ForLoop value, Context context) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Item visitForList(ForList value, Context context) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Item visitReturnStatement(ReturnStatement value, Context context) {
		List<Expression> list = value.expressions.list;
		int size = list.size();
		if (size > 0) {
			int base = context.allocate(size);
			for (int i = 0; i < size; i++) {
				list.get(i).visit(this, context)
						.load(base + i);
			}
			context.w.write1(RETURN, size, base);
		} else {
			context.w.write1(RETURN, 0, 0);
		}
		return null;
	}

	@Override
	public Item visitUnaryOp(UnaryOp value, Context context) {
		Item argument = value.arg.visit(this, context);
		if (value.op == UnaryOp.OP_NOT) {
			CondItem cond = argument.cond();
			cond.op = NOT;
//			int target = cond.jumpFalse();
//			cond.insertTrueTarget();
			return cond;
		}
		return new UnaryItem(context, argument, value.op);
	}

	@Override
	public Item visitBinaryOp(BinaryOp value, Context context) {
		if (value.op == BinaryOp.OP_AND) {
			CondItem lCond = value.left.visit(this, context)
					.cond();
			int target = lCond.jumpFalse();
			lCond.insertTrueTarget();
			CondItem rCond = value.right.visit(this, context)
					.cond();
			return new CondItem(context, rCond, target);
		} else if (value.op == BinaryOp.OP_OR) {
			CondItem lCond = value.left.visit(this, context)
					.cond();
			int target = lCond.jumpTrue();
			lCond.insertFalseTarget();
			CondItem rCond = value.right.visit(this, context)
					.cond();
			return new CondItem(context, rCond, target);
		}
		// @TODO Jezza - 09 Apr 2018: Constant folding?
		Item left = value.left.visit(this, context);
		Item right = value.right.visit(this, context);
		return new BinaryItem(context, left, value.op, right);
	}

	@Override
	public Item visitLiteral(Literal value, Context context) {
		switch (value.type) {
			case Literal.STRING:
			case Literal.DOUBLE:
			case Literal.INTEGER:
				return new ConstantItem(context, value.value);
			case Literal.FALSE:
				return new ConstantItem(context, Boolean.FALSE);
			case Literal.TRUE:
				return new ConstantItem(context, Boolean.TRUE);
			case Literal.NIL:
				return new ConstantItem(context, null);
			default:
				throw new IllegalStateException("Unknown literal type: " + value.type);
		}
	}

	@Override
	public Item visitName(Name value, Context context) {
		return new NameItem(context, value);
	}

	@Override
	public Item visitVarargs(Varargs value, Context context) {
//		if (value.expectedResults == Varargs.UNBOUNDED) {
//			int target = context.mark();
//			context.w.write2(OpCode.VARARGS, -1, target);
//			return null;
//		}
//		for (int i = 0, l = value.expectedResults; i < l; i++) {
//			int target = context.allocate();
//			context.w.write2(OpCode.VARARGS, i, target);
//		}
//		return null;
		throw new IllegalStateException("NYI");
	}

	@Override
	public Item visitTableConstructor(TableConstructor value, Context context) {
		List<Item> fields = new ArrayList<>(2);
		for (TableField field : value.fields) {
			fields.add(field.visit(this, context));
		}
		return new TableItem(context, fields);
	}

	@Override
	public Item visitTableField(TableField value, Context context) {
		Item keyItem = value.key.visit(this, context);
		Item valueItem = value.value.visit(this, context);
		return new TableFieldItem(context, keyItem, valueItem);
	}

	static abstract class Item {
		protected final Context context;

		Item(Context context) {
			this.context = context;
		}

		public Item load(int register) {
			throw new IllegalStateException("load not supported on " + getClass().getSimpleName());
		}

		public void store(Item item) {
			throw new IllegalStateException("store not supported on " + getClass().getSimpleName());
		}

		public void set(int register) {
			throw new IllegalStateException("set(1) not supported on " + getClass().getSimpleName());
		}

		public void set(Item key, Item value) {
			throw new IllegalStateException("set(2) not supported on " + getClass().getSimpleName());
		}

		public int address() {
//			throw new IllegalStateException("address not supported on " + getClass().getSimpleName());
			return -1;
		}

		public InvokeResultItem invoke(int base, int count, int expected) {
			throw new IllegalStateException("invoke not supported on " + getClass().getSimpleName());
		}

		public void drop() {
		}

		public CondItem cond() {
			int addr = address();
			if (addr == -1) {
				// We don't need to allocate it, because it'll be quickly used then freed.
				addr = context.mark();
				load(addr);
			}
			return new CondItem(context, addr);
//			throw new IllegalStateException("invoke not supported on " + getClass().getSimpleName());
		}
	}

	static final class RegisterItem extends Item {
		private final int register;

		RegisterItem(Context context, int register) {
			super(context);
			this.register = register;
		}

		@Override
		public Item load(int register) {
			context.w.write2(MOVE, this.register, register);
			return new RegisterItem(context, register);
		}

		@Override
		public InvokeResultItem invoke(int base, int count, int expected) {
			context.w.write2(CALL, base, count, expected);
			// Pop the arguments and the function
			context.pop(count + 1);
			// Allocate the expected results.
			context.allocate(expected);
			return new InvokeResultItem(context, base, expected);
		}

		@Override
		public int address() {
			return register;
		}
	}

	static final class NameItem extends Item {
		private final Name name;

		NameItem(Context context, Name name) {
			super(context);
			this.name = name;
		}

		@Override
		public int address() {
			return name.is(FLAG_LOCAL)
					? name.index
					: -1;
		}

		@Override
		public Item load(int register) {
			Name name = this.name;
			if (name.is(FLAG_LOCAL)) {
				context.w.write2(MOVE, name.index, register);
				return new RegisterItem(context, register);
			} else if (name.is(FLAG_GLOBAL)) {
				int constant = context.loadConstant(name.value);
				context.w.write2(GET_GLOBAL, constant, register);
				context.pop();
				return new RegisterItem(context, register);
			} else if (name.is(FLAG_UPVAL)) {
				int index = context.upvalue(name);
				context.w.write2(GET_UPVAL, index, register);
				return new RegisterItem(context, register);
			} else {
				throw new IllegalStateException("Invalid name state: " + name);
			}
		}

		@Override
		public void store(Item item) {
			Name name = this.name;
			if (name.is(FLAG_LOCAL)) {
				item.load(name.index);
			} else if (name.is(FLAG_GLOBAL)) {
				int register = context.allocate();
				item.load(register);
				int constant = context.loadConstant(name.value);
				context.w.write2(SET_GLOBAL, constant, register);
				context.pop();
			} else if (name.is(FLAG_UPVAL)) {
				int register = context.allocate();
				item.load(register);
				int index = context.upvalue(name);
				context.w.write2(SET_UPVAL, register, index);
			} else {
				throw new IllegalStateException("Invalid name state: " + name);
			}
		}
	}

	static final class InvokeResultItem extends Item {
		public final int base;
		public final int expected;

		InvokeResultItem(Context context, int base, int expected) {
			super(context);
			this.base = base;
			this.expected = expected;
		}

		@Override
		public int address() {
			if (expected != 1) {
				throw new IllegalStateException("Expected multiple results: " + expected);
			}
			return base;
		}

		public void unpack(List<Item> items) {
			if (expected > 0) {
				int base = this.base;
				for (int i = 0; i < expected; i++) {
					items.add(new RegisterItem(context, base + i));
				}
			}
		}
	}

	static final class UnaryItem extends Item {
		private final Item value;
		private final int op;

		UnaryItem(Context context, Item value, int op) {
			super(context);
			this.value = value;
			this.op = op;
		}

		@Override
		public Item load(int register) {
			switch (op) {
				case UnaryOp.OP_MINUS:
					return makeUnary(NEG, register);
				case UnaryOp.OP_LEN:
					return makeUnary(LEN, register);
				case UnaryOp.OP_TO_NUMBER:
					return makeUnary(TO_NUMBER, register);
				case UnaryOp.OP_TO_STRING:
					return makeUnary(TO_STRING, register);
				case UnaryOp.OP_NOT:
					assert false;
				default:
					throw new IllegalStateException("Unsupported: " + op);
			}
		}

		private Item makeUnary(int op, int register) {
			int addr = value.address();
			if (addr == -1) {
				// Don't bother allocating, as we're just gonna clear it right as we exit.
				addr = context.mark();
				value.load(addr);
			}
			context.w.write2(op, register, addr);
			return new RegisterItem(context, register);
		}
	}

	static final class BinaryItem extends Item {
		private final Item left;
		private final int op;
		private final Item right;

		BinaryItem(Context context, Item left, int op, Item right) {
			super(context);
			this.left = left;
			this.op = op;
			this.right = right;
		}

		@Override
		public void store(Item item) {
			switch (op) {
				case BinaryOp.OP_INDEXED: {
					int allocated = 0;
					int table = left.address();
					if (table == -1) {
						table = context.allocate();
						left.load(table);
						allocated++;
					}
					int key = right.address();
					if (key == -1) {
						key = context.allocate();
						right.load(key);
						allocated++;
					}
					int value = item.address();
					if (value == -1) {
						value = context.allocate();
						item.load(value);
						allocated++;
					}
					context.w.write2(SET_TABLE, table, key, value);
					if (allocated > 0) {
						context.pop(allocated);
					}
					return;
				}
				default:
					throw new IllegalStateException("Store called on " + op);
			}
		}

		@Override
		public Item load(int register) {
			switch (op) {
				case BinaryOp.OP_ADD:
					return makeBinary(ADD, register, false);
				case BinaryOp.OP_SUB:
					return makeBinary(SUB, register, false);
				case BinaryOp.OP_MUL:
					return makeBinary(MUL, register, false);
				case BinaryOp.OP_DIV:
					return makeBinary(DIV, register, false);
				case BinaryOp.OP_MOD:
					return makeBinary(MOD, register, false);
				case BinaryOp.OP_POW:
					return makeBinary(POW, register, false);
				case BinaryOp.OP_CONCAT:
					return makeBinary(CONCAT, register, false);
				case BinaryOp.OP_NE:
					// NQ e1 e2 = N(EQ) e1 e2
					return makeBinary(EQ, register, true);
				case BinaryOp.OP_EQ:
					// EQ e1 e2 = EQ e1 e2
					return makeBinary(EQ, register, false);
				case BinaryOp.OP_LT:
					// LT e1 e2 = LT e1 e2
					return makeBinary(LT, register, false);
				case BinaryOp.OP_LE:
					// LE e1 e2 = LE e1 e2
					return makeBinary(LE, register, false);
				case BinaryOp.OP_GT:
					// GT e1 e2 = LT e2 e1
					return makeBinary(LT, register, true);
				case BinaryOp.OP_GE:
					// GE e1 e2 = LE e2 e1
					return makeBinary(LE, register, true);
				case BinaryOp.OP_AND:
				case BinaryOp.OP_OR:
					throw new IllegalStateException("Inconsistent compiler state: [Operation should be handled earlier] " + op);
				case BinaryOp.OP_INDEXED:
					return makeBinary(GET_TABLE, register, false);
				default:
					throw new IllegalStateException("Unsupported: " + op);
			}
		}

		private Item makeBinary(int op, int register, boolean swap) {
			int allocated = 0;
			int leftAddr = left.address();
			if (leftAddr == -1) {
				leftAddr = context.allocate();
				left.load(leftAddr);
				allocated++;
			}
			int rightAddr = right.address();
			if (rightAddr == -1) {
				rightAddr = context.allocate();
				right.load(rightAddr);
				allocated++;
			}
			if (swap) {
				int temp = rightAddr;
				rightAddr = leftAddr;
				leftAddr = temp;
			}
			context.w.write2(op, register, leftAddr, rightAddr);
			if (allocated > 0) {
				context.pop(allocated);
			}
			return new RegisterItem(context, register);
		}
	}

	static final class TableItem extends Item {
		private final List<Item> fields;

		TableItem(Context context, List<Item> fields) {
			super(context);
			this.fields = fields;
		}

		@Override
		public Item load(int register) {
			context.w.write2(NEW_TABLE, register);
			for (Item field : fields) {
				field.set(register);
			}
			return new RegisterItem(context, register);
		}
	}

	static final class TableFieldItem extends Item {
		private final Item key;
		private final Item value;

		TableFieldItem(Context context, Item key, Item value) {
			super(context);
			this.key = key;
			this.value = value;
		}

		@Override
		public void set(int register) {
			int key = context.allocate(2);
			int value = key + 1;

			this.key.load(key);
			this.value.load(value);

			context.w.write2(SET_TABLE, register, key, value);
		}
	}

	static final class CondItem extends Item {
		private final int register;
		public int op;

		private int trueJumpList;
		private int falseJumpList;

		CondItem(Context context, int register) {
			super(context);
			this.register = register;
			op = TEST;
			trueJumpList = -1;
			falseJumpList = -1;
		}

		CondItem(Context context, CondItem item, int jumpList) {
			super(context);
			register = item.register;
			op = item.op;
			trueJumpList = item.trueJumpList;
			falseJumpList = jumpList; // item.falseJumpList;
//			code[item.falseJumpList] = jumpList
//			code[jumpList] = item.falseJumpList
		}

		@Override
		public CondItem cond() {
			return this;
		}

		public int jumpTrue() {
			return jump(0, trueJumpList);
		}

		public int jumpFalse() {
			return jump(1, falseJumpList);
		}

		private int jump(int complement, int list) {
			ByteCodeWriter w = context.w;
			w.write2(op, register);
			int mark = w.reserve2();
			w.write1(complement);
			if (list != -1) {
				w.patch2(mark, list);
			}
			return mark;
		}

		public void insertTrueTarget() {
			if (trueJumpList != -1) {
				backpatchList(trueJumpList);
			}
		}

		public void insertFalseTarget() {
			if (falseJumpList != -1) {
				backpatchList(falseJumpList);
			}
		}

		private void backpatchList(int list) {
			ByteCodeWriter w = context.w;
			int mark = w.mark();
			int target = list;
			do {
				int next = w.get2(target);
				w.patch2(target, mark);
				target = next;
			} while (target != -1);
		}
	}

	static final class ConstantItem extends Item {
		private final Object value;

		ConstantItem(Context context, Object value) {
			super(context);
			this.value = value;
		}

		@Override
		public Item load(int register) {
			if (value == Boolean.TRUE) {
				context.w.write2(CONST_TRUE, register);
			} else if (value == Boolean.FALSE) {
				context.w.write2(CONST_FALSE, register);
			} else if (value == null) {
				context.w.write2(CONST_NIL, register);
			} else if (value instanceof LuaChunk) {
				int constant = context.pool.add(value);
				context.w.write2(LOAD_FUNC, constant, register);
			} else {
				int constant = context.pool.add(value);
				context.w.write2(CONST, constant, register);
			}
			return new RegisterItem(context, register);
		}
	}

	static final class ChainItem extends Item {
		private final List<Item> items;

		ChainItem(Context context, List<Item> items) {
			super(context);
			this.items = items;
		}
	}
}
