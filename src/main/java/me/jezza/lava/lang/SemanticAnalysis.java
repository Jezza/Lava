package me.jezza.lava.lang;

import static me.jezza.lava.lang.ParseTree.Block.FLAG_NEW_CONTEXT;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_CHECKED;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_GLOBAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_LOCAL;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_UNCHECKED;
import static me.jezza.lava.lang.ParseTree.Name.FLAG_UPVAL;

import java.util.ArrayList;
import java.util.List;

import me.jezza.lava.lang.ParseTree.Block;
import me.jezza.lava.lang.ParseTree.FunctionBody;
import me.jezza.lava.lang.ParseTree.Name;

/**
 * @author Jezza
 */
public final class SemanticAnalysis extends AbstractScanner<Block, Object> {

	public static void run(FunctionBody node) {
		SemanticAnalysis phase = new SemanticAnalysis();
		phase.scan(node.body, node.body);
	}

	private void prepBlock(Block block, Block parent) {
		block.names = new ArrayList<>();
		if (block != parent) {
			block.parent = parent;
			if (!block.is(FLAG_NEW_CONTEXT)) {
				block.offset = parent.names.size() + parent.offset;
			}
		}
	}

	@Override
	public Object visitBlock(Block value, Block userObject) {
		prepBlock(value, userObject);
		return super.visitBlock(value, value);
	}

	@Override
	public Object visitFunctionBody(FunctionBody value, Block userObject) {
		Block block = value.body;
		prepBlock(block, userObject);
		scan(value.parameters, block);
		return super.visitBlock(block, block);
	}

//	@Override
//	public Object visitFunctionCall(FunctionCall value, Block userObject) {
//		Object returnValue = scan(value.target, userObject);
//
//		Iterator<Expression> it = value.args.list.iterator();
//		while (it.hasNext()) {
//			Expression argument = it.next();
//			boolean last = !it.hasNext();
//			if (argument instanceof FunctionCall) {
//				((FunctionCall) argument).expectedResults = last
//						? 1 // FunctionCall.VARARGS
//						: 1;
//			} else if (argument instanceof Varargs) {
//				((Varargs) argument).expectedResults = last
//						? 1 // -1
//						: 1;
//			}
//			returnValue = scanThenReduce(argument, userObject, returnValue);
//		}
//		return returnValue;
//	}

//	@Override
//	public Object visitAssignment(Assignment value, Block userObject) {
//		if (value.lhs != null) {
//			ExpressionList lhs = value.lhs;
//			ExpressionList rhs = value.rhs;
//			a = {}
//			b = {}
//	
//			function value(val)
//					print("v:" .. val);
//					return val;
//			end
//
//			a[value(0)], b[value(1)] = value(2), value(3)
//	
//			print("---")
//			local a__1 = value(0)
//			local b__1 = value(1)
//			a[a__1] = value(2)
//			b[b__1] = value(3)
//
//		}
//		return super.visitAssignment(value, userObject);
//	}

	@Override
	public Object visitName(Name value, Block userObject) {
		// This is only used because we don't have proper AST copying. (take a look at the while loop)
		if (value.is(FLAG_CHECKED)) {
			return null;
		}
		if (value.is(FLAG_UNCHECKED)) {
			value.set(FLAG_UNCHECKED, false);
			value.index = find(value, userObject);
		} else if (value.is(FLAG_LOCAL)) {
			int index = indexOf(value, userObject);
			if (index == -1) {
				index = userObject.offset + userObject.names.size();
				userObject.names.add(value);
			}
			value.index = index;
		} else {
			// Checked, but non-local?
			// This shouldn't happen...
			throw new IllegalStateException("Possible assertion fail: " + value);
		}
		value.set(FLAG_CHECKED, true);
		return null;
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
