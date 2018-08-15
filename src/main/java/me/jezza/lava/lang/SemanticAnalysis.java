package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.Block.FLAG_CONTROL_FLOW_BARRIER;
import static me.jezza.lava.lang.ParseTree.Block.FLAG_CONTROL_FLOW_EXIT;
import static me.jezza.lava.lang.ParseTree.Block.FLAG_NEW_CONTEXT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_GLOBAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_UPVAL;

import java.util.ArrayList;
import java.util.List;

import me.jezza.lava.lang.ParseTree.Assignment;
import me.jezza.lava.lang.ParseTree.Block;
import me.jezza.lava.lang.ParseTree.Break;
import me.jezza.lava.lang.ParseTree.Expression;
import me.jezza.lava.lang.ParseTree.FunctionBody;
import me.jezza.lava.lang.ParseTree.Name;
import me.jezza.lava.lang.model.AbstractTranslator;

/**
 * @author Jezza
 */
public final class SemanticAnalysis extends AbstractTranslator<Block> {

	public static void run(FunctionBody node) {
		SemanticAnalysis phase = new SemanticAnalysis();
		phase.translate(node.body, node.body);
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
	public ParseTree visitBlock(Block value, Block userObject) {
		prepBlock(value, userObject);
		return super.visitBlock(value, value);
	}

	@Override
	public ParseTree visitFunctionBody(FunctionBody value, Block userObject) {
		Block block = value.body;
		prepBlock(block, userObject);
		translate(value.parameters, block);
		// Skip the direct scan, as it'll just refire the prepBlock method.
		return super.visitBlock(block, block);
	}

	@Override
	public ParseTree visitAssignment(Assignment value, Block userObject) {
		if (value.lhs != null) {
			List<String> names = new ArrayList<>();
			for (Expression expression : value.lhs.list) {
				if (expression instanceof Name && expression.is(FLAG_LOCAL)) {
					names.add(((Name) expression).value);
				}
			}
			System.out.println(names);
		}
		return super.visitAssignment(value, userObject);
	}

	@Override
	public ParseTree visitBreak(Break value, Block userObject) {
		while (!userObject.is(FLAG_CONTROL_FLOW_BARRIER) && !userObject.is(FLAG_CONTROL_FLOW_EXIT)) {
			userObject = userObject.parent;
		}
		System.out.println(userObject);
		return value;
	}

	@Override
	public ParseTree visitName(Name value, Block userObject) {
		if (value.index != -1) {
			return value;
		}
		if (value.is(FLAG_LOCAL)) {
			int index = indexOf(value, userObject);
			if (index == -1) {
				index = userObject.offset + userObject.names.size();
				userObject.names.add(value);
			}
			value.index = index;
		} else {
			value.index = find(value, userObject);
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
