package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.Block.FLAG_CONTROL_FLOW_BARRIER;
import static me.jezza.lava.lang.ParseTree.Block.FLAG_CONTROL_FLOW_EXIT;
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
import me.jezza.lava.lang.ParseTree.Break;
import me.jezza.lava.lang.ParseTree.Expression;
import me.jezza.lava.lang.ParseTree.FunctionBody;
import me.jezza.lava.lang.ParseTree.Goto;
import me.jezza.lava.lang.ParseTree.Label;
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
		List<Goto> jumps = new ArrayList<>(0);

		Block exit;

		Context(Block block) {
			this.block = block;
		}

		void register(Name name) {
			data.add(new NameData(name.value, index));
		}

		void mark(Label newLabel) {
			String name = newLabel.name;
			Block block = this.block;
			List<Label> labels = block.labels;
			for (Label label : labels) {
				if (label.name.equals(name)) {
					return;
				}
			}
			labels.add(newLabel);

			var it = jumps.iterator();
			while (it.hasNext()) {
				Goto value = it.next();
				if (name.equals(value.label)) {
					value.resolvedLabel = newLabel;
					it.remove();
				}
			}
		}

		void mark(Goto value) {
			jumps.add(value);
		}

		Label resolveLabel(String name) {
			Block block = this.block;
			while (block != null && !block.is(FLAG_CONTROL_FLOW_BARRIER)) {
				for (Label label : block.labels) {
					if (label.name.equals(name)) {
						return label;
					}
				}
				block = block.parent;
			}
			return null;
		}

		Block pushBlock(Block block) {
//			System.out.println("Pushing : " + block);
			Block parent = this.block;
			if (block != parent) {
				block.parent = parent;
				if (!block.is(FLAG_NEW_CONTEXT)) {
					block.offset = parent.names.size() + parent.offset;
				}
			}
			if (block.is(FLAG_CONTROL_FLOW_EXIT)) {
				exit = block;
			} else if (block.is(FLAG_CONTROL_FLOW_BARRIER)) {
				exit = null;
			}
			this.block = block;
			return parent;
		}

		Block popBlock(Block parent) {
//			System.out.println("Popping : " + block);
			assert block.parent == null && block == parent || block.parent == parent : "Current: " + block + " :: Parent: " + block.parent + " :: Old: " + parent;
			Block block = this.block;
			this.block = parent;
			exit = null;
			if (parent.is(FLAG_CONTROL_FLOW_BARRIER)) {
				throw new IllegalStateException("Unresolved goto(s): " + jumps);
			}
			return block;
		}

		Block exit() {
			if (exit == null) {
				Block block = this.block;
				if (block.is(FLAG_CONTROL_FLOW_BARRIER)) {
					return null;
				}
				Block current = block.parent;
				while (current != null && !current.is(FLAG_CONTROL_FLOW_EXIT)) {
					current = !current.is(FLAG_CONTROL_FLOW_BARRIER)
							? current.parent
							: null;
				}
				exit = current;
			}
			return exit;
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

	@Override
	public ParseTree visitBreak(Break value, Context context) {
		Block exit = context.exit();
		if (exit == null) {
			throw new IllegalStateException("<break> not inside loop");
		}
//		exit.set(FLAG_CONTROL_FLOW_VALID, true);
		String name = "label::".concat(Integer.toString(exit.hashCode()));
		return new Goto(name);
	}

	@Override
	public ParseTree visitLabel(Label value, Context context) {
		context.mark(value);
		return value;
	}

	@Override
	public ParseTree visitGoto(Goto value, Context context) {
		Label resolved = context.resolveLabel(value.label);
		if (resolved != null) {
			value.resolvedLabel = resolved;
		} else {
			context.mark(value);
		}
		return value;
	}

	@Override
	public ParseTree visitBlock(Block value, Context context) {
		Block old = context.pushBlock(value);
		ParseTree node = super.visitBlock(value, context);
		context.popBlock(old);
		return node;
	}

	@Override
	public ParseTree visitFunctionBody(FunctionBody value, Context context) {
		Block block = value.body;
		Block old = context.pushBlock(block);
		translate(value.parameters, context);
		// Skip the direct scan, as it'll just refire the prepBlock method.
		var newBlock = super.visitBlock(block, context);
		if (block != newBlock) {
			value.body = block;
		}
		context.popBlock(old);
		return value;
	}

	@Override
	public ParseTree visitRepeatBlock(RepeatBlock value, Context context) {
		Block block = value.body;
		Block old = context.pushBlock(block);
		// Skip the direct scan, as it'll just refire the prepBlock method.
		super.visitBlock(block, context);
		var oldCondition = value.condition;
		var newCondition = translate(oldCondition, context);
		if (oldCondition != newCondition) {
			value.condition = newCondition;
		}
		context.popBlock(old);
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

	@Override
	public ParseTree visitName(Name value, Context context) {
		if (value.index == -1) {
			int old = value.index;
			Block block = context.block;
			if (value.is(FLAG_LOCAL)) {
				int index = indexOf(block, value);
				if (index == -1) {
					index = block.offset + block.names.size();
					block.names.add(value);
				}
				value.index = index;
			} else {
				value.index = find(block, value);
			}
		}
		int i = context.index;
		if (value.is(FLAG_ASSIGNMENT)) {
			assert i >= 0 : "Illegal compiler assertion";
			context.register(value);
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

	private int find(Block block, Name name) {
		int index = indexOf(block, name);
		if (index >= 0) {
			name.set(FLAG_LOCAL, true);
			return index;
		}
		if (block.parent != null) {
			index = find(block.parent, name);
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

	private int indexOf(Block block, Name name) {
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
