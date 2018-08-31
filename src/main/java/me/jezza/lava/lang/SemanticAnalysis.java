package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.Block.FLAG_NEW_CONTEXT;
import static me.jezza.lava.lang.ParseTree.FLAG_ASSIGNMENT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_GLOBAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_UPVAL;

import java.util.ArrayList;
import java.util.List;

import me.jezza.lava.Strings;
import me.jezza.lava.lang.ParseTree.Assignment;
import me.jezza.lava.lang.ParseTree.Block;
import me.jezza.lava.lang.ParseTree.Expression;
import me.jezza.lava.lang.ParseTree.FunctionBody;
import me.jezza.lava.lang.ParseTree.Name;
import me.jezza.lava.lang.ParseTree.RepeatBlock;
import me.jezza.lava.lang.SemanticAnalysis.Context;
import me.jezza.lava.lang.model.AbstractTranslator;

/**
 * @author Jezza
 */
public final class SemanticAnalysis extends AbstractTranslator<Context> {

	private static final String CONFLICT_PREFIX = "$";

	static final class Context {
		Block block;
		int index = -1;
		List<NameData> data = new ArrayList<>(0);

		Context(Block block) {
			this.block = block;
		}

		void push(Name name) {
			data.add(new NameData(name.value, index));
		}
	}

	static final class NameData {
		final String value;
		final int index;

		Name replacement;
		Name insert;

		NameData(String value, int index) {
			this.value = value;
			this.index = index;
		}

		@Override
		public String toString() {
			return Strings.format("NameData{value={}, index={}}",
					value,
					index);
		}
	}

	public static void run(FunctionBody node) {
		SemanticAnalysis phase = new SemanticAnalysis();
		phase.translate(node.body, new Context(node.body));
	}

	private void prepBlock(Block block, Block parent) {
		block.names = new ArrayList<>(0);
		if (block != parent) {
			block.parent = parent;
			if (!block.is(FLAG_NEW_CONTEXT)) {
				block.offset = parent.names.size() + parent.offset;
			}
		}
	}

	@Override
	public ParseTree visitBlock(Block value, Context context) {
		prepBlock(value, context.block);
		Block old = context.block;
		context.block = value;
		ParseTree node = super.visitBlock(value, context);
		context.block = old;
		return node;
	}

	@Override
	public ParseTree visitFunctionBody(FunctionBody value, Context context) {
		Block block = value.body;
		prepBlock(block, context.block);
		Block old = context.block;
		context.block = block;
		translate(value.parameters, context);
		// Skip the direct scan, as it'll just refire the prepBlock method.
		ParseTree node = super.visitBlock(block, context);
		context.block = old;
		return node;
	}

	@Override
	public ParseTree visitRepeatBlock(RepeatBlock value, Context context) {
		Block block = value.body;
		prepBlock(block, context.block);
		Block old = context.block;
		context.block = block;
		// Skip the direct scan, as it'll just refire the prepBlock method.
		super.visitBlock(block, context);
		var oldCondition = value.condition;
		var newCondition = translate(oldCondition, context);
		if (oldCondition != newCondition) {
			value.condition = newCondition;
		}
		context.block = old;
		return value;
	}

	@Override
	public ParseTree visitAssignment(Assignment value, Context context) {
		assert context.index == -1 : "Recursive assignment node : " + context.index;
		List<Expression> left = null;
		if (value.lhs != null) {
			left = value.lhs.list;
			transform(left, context);
		}
		var right = value.rhs.list;
		transform(right, context);
		if (left != null) {
			for (NameData datum : context.data) {
				Name insert = datum.insert;
				if (insert != null) {
					String newName = CONFLICT_PREFIX.concat(datum.value);

					Name name = new Name(newName, FLAG_LOCAL | FLAG_ASSIGNMENT);
					visitName(name, context);

					left.add(datum.index, name);
					right.add(datum.index, insert);
				}
			}
		}
		context.data.clear();
		return value;
	}

	private void transform(List<Expression> expressions, Context context) {
		for (int i = 0, l = expressions.size(); i < l; i++) {
			var oldExpr = expressions.get(i);
			context.index = i;
			var newExpr = translate(oldExpr, context);
			if (newExpr != oldExpr) {
				expressions.set(i, newExpr);
			}
		}
		context.index = -1;
	}

//	@Override
//	public ParseTree visitBreak(Break value, Block userObject) {
//		while (!userObject.is(FLAG_CONTROL_FLOW_BARRIER) && !userObject.is(FLAG_CONTROL_FLOW_EXIT)) {
//			userObject = userObject.parent;
//		}
//		System.out.println(userObject);
//		return value;
//	}

	@Override
	public ParseTree visitName(Name value, Context context) {
		if (value.index == -1) {
			Block block = context.block;
			if (value.is(FLAG_LOCAL)) {
				int index = indexOf(value, block);
				if (index == -1) {
					index = block.offset + block.names.size();
					block.names.add(value);
				}
				value.index = index;
			} else {
				value.index = find(value, block);
			}
		}
		int i = context.index;
		if (i >= 0 && value.is(FLAG_ASSIGNMENT)) {
			context.push(value);
		}
		for (NameData datum : context.data) {
			if (datum.value.equals(value.value)) {
				int index = datum.index;
				if (index < i) {
//					System.out.println(" CONFLICT :: " + value + " :: " + index + " :: " + i);
					var replacement = datum.replacement;
					if (replacement == null) {
						String newName = CONFLICT_PREFIX.concat(datum.value);
						replacement = new Name(newName, FLAG_LOCAL);
						visitName(replacement, context);
						datum.replacement = replacement;
					}
					assert datum.insert == null : "Insert is already occupied.";
					datum.insert = value;
					return replacement;
				}
			}
		}
		return value;
	}

	private int find(Name name, Block block) {
		int index = indexOf(name, block);
		if (index >= 0) {
			name.set(FLAG_LOCAL, true);
			return index;
		}
		if (block.parent != null) {
			index = find(name, block.parent);
			if (name.is(FLAG_GLOBAL)) {
				return -1;
			}
			if (block.is(FLAG_NEW_CONTEXT)) {
				name.set(FLAG_LOCAL, false);
				name.set(FLAG_UPVAL, true);
				name.level++;
			}
			return index;
		}
		name.set(FLAG_GLOBAL, true);
		return -1;
	}

	private int indexOf(Name name, Block block) {
		List<Name> names = block.names;
		for (int i = 0, size = names.size(); i < size; i++) {
			Name other = names.get(i);
			if (other.value.equals(name.value)) {
				return block.offset + i;
			}
		}
		return -1;
	}
}
