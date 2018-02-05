package me.jezza.lava.lang;

import java.util.ArrayList;
import java.util.List;

import me.jezza.lava.lang.SemanticPhase.Context;
import me.jezza.lava.lang.ast.ParseTree.Assignment;
import me.jezza.lava.lang.ast.ParseTree.Block;
import me.jezza.lava.lang.ast.ParseTree.Literal;

/**
 * @author Jezza
 */
public final class SemanticPhase extends AbstractVisitor<Context, Object> {
	static final class Context {
		List<String> names;
	}

	@Override
	public Object visitAssignment(Assignment value, Context userObject) {
		userObject.names = new ArrayList<>();
		value.lhs.visit(this, userObject);
		List<String> left = userObject.names;

		userObject.names = new ArrayList<>();
		value.rhs.visit(this, userObject);
		List<String> right = userObject.names;
		userObject.names = null;

		System.out.println(left + " :: " + right);
		return null;
	}

	@Override
	public Object visitLiteral(Literal value, Context userObject) {
		if (value.type == Literal.NAMESPACE) {
			userObject.names.add((String) value.value);
		}
		return null;
	}

	public static void run(Block block) {
		block.visit(new SemanticPhase(), new Context());
	}
}
