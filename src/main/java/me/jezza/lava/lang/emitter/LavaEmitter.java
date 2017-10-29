package me.jezza.lava.lang.emitter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import me.jezza.lava.lang.LavaLexer;
import me.jezza.lava.lang.LavaParser;
import me.jezza.lava.lang.Tokens;
import me.jezza.lava.lang.ast.Tree.Assignment;
import me.jezza.lava.lang.ast.Tree.BinaryOp;
import me.jezza.lava.lang.ast.Tree.Block;
import me.jezza.lava.lang.ast.Tree.Break;
import me.jezza.lava.lang.ast.Tree.DoBlock;
import me.jezza.lava.lang.ast.Tree.Expression;
import me.jezza.lava.lang.ast.Tree.ExpressionList;
import me.jezza.lava.lang.ast.Tree.ForList;
import me.jezza.lava.lang.ast.Tree.ForLoop;
import me.jezza.lava.lang.ast.Tree.FunctionBody;
import me.jezza.lava.lang.ast.Tree.FunctionCall;
import me.jezza.lava.lang.ast.Tree.FunctionName;
import me.jezza.lava.lang.ast.Tree.FunctionStatement;
import me.jezza.lava.lang.ast.Tree.Goto;
import me.jezza.lava.lang.ast.Tree.IfBlock;
import me.jezza.lava.lang.ast.Tree.Label;
import me.jezza.lava.lang.ast.Tree.Literal;
import me.jezza.lava.lang.ast.Tree.LocalFunction;
import me.jezza.lava.lang.ast.Tree.LocalStatement;
import me.jezza.lava.lang.ast.Tree.ParameterList;
import me.jezza.lava.lang.ast.Tree.RepeatBlock;
import me.jezza.lava.lang.ast.Tree.ReturnStatement;
import me.jezza.lava.lang.ast.Tree.Statement;
import me.jezza.lava.lang.ast.Tree.TableConstructor;
import me.jezza.lava.lang.ast.Tree.TableField;
import me.jezza.lava.lang.ast.Tree.UnaryOp;
import me.jezza.lava.lang.ast.Tree.Varargs;
import me.jezza.lava.lang.ast.Tree.Variable;
import me.jezza.lava.lang.ast.Tree.WhileLoop;
import me.jezza.lava.lang.emitter.LavaEmitter.Scope;
import me.jezza.lava.lang.interfaces.Visitor.PVisitor;
import me.jezza.lava.runtime.Interpreter;
import me.jezza.lava.runtime.Interpreter.LuaChunk;
import me.jezza.lava.runtime.OpCodes;

/**
 * @author Jezza
 */
public final class LavaEmitter implements PVisitor<Scope> {
	public static void main(String[] args) throws Throwable {
		LuaChunk chunk = nom("test.lua");
		Interpreter.test(chunk);
	}

	static final class Scope {
		final String name;
		final Scope previous;
		final ByteCodeWriter w;
		final ConstantPool pool;
		final ConstantPool locals;

		int top;
		int results;

		public Scope(String name) {
			this(name, null);
		}

		public Scope(String name, Scope previous) {
			this.name = name;
			this.previous = previous;
			this.w = new ByteCodeWriter();
			this.pool = new ConstantPool();
			locals = new ConstantPool();
		}

		public int registerLocal(String name) {
			return locals.add(name);
		}

		public Scope newScope(String name) {
			return new Scope(name, this);
		}

		public LuaChunk build() {
			LuaChunk chunk = new LuaChunk(name);
			chunk.constants = pool.build();
			if (w.get(w.mark()) != OpCodes.RET) {
				w.write1(OpCodes.RET);
			}
			chunk.code = w.code();
			// chunk.chunks = chunks.toArray(new LuaChunk[0]);
			return chunk;
		}
	}

//	class Block:
//		int breaklist;      /* list of jumps out of this loop */
//		int nactvar;        /* # active locals outside the breakable structure */
//		boolean upval;      /* true if some variable in the block is an upvalue */
//		boolean isbreakable;/* true if `block' is a loop */

	@Override
	public Void visitFunctionCall(FunctionCall value, Scope scope) {
		// Load args
		value.args.visit(this, scope);
		// Load function
		for (Expression prefix : value.prefix) {
			prefix.visit(this, scope);
		}
		// Call function
		int count = value.args instanceof ExpressionList
					? ((ExpressionList) value.args).list.size()
					: 1;
		scope.w.write2(OpCodes.CALL, count, scope.results);
		return null;
	}

	private void move(String name, Scope scope) {
		int index = scope.registerLocal(name);
		scope.w.write2(OpCodes.MOV, scope.top - 1, index);
		if (scope.top - 1 != index) {
			scope.top--;
			scope.w.write1(OpCodes.POP);
		}
	}

	@Override
	public Void visitVariable(Variable variable, Scope scope) {
		move(variable.name, scope);
		return null;
	}

	@Override
	public Void visitAssignment(Assignment value, Scope scope) {
		if (value.lhs == null) {
			value.rhs.visit(this, scope);
		} else {
			List<Expression> lhs = value.lhs.list;
			List<Expression> rhs = value.rhs.list;

			int ls = lhs.size();
			int rs = rhs.size();
			if (ls > rs) {
				for (int i = 0; i < ls; i++) {
					if (i >= rs) {
						scope.w.write1(OpCodes.CONST_NIL);
						scope.top++;
					} else {
						rhs.get(i).visit(this, scope);
					}
					lhs.get(i).visit(this, scope);
				}
			} else {
				for (int i = 0; i < ls; i++) {
					rhs.get(i).visit(this, scope);
					lhs.get(i).visit(this, scope);
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
		if (value.lhs == null) {
			value.rhs.visit(this, scope);
		} else {
			List<String> lhs = value.lhs;
			List<Expression> rhs = value.rhs.list;

			int ls = lhs.size();
			int rs = rhs.size();
			if (ls > rs) {
				for (int i = 0; i < ls; i++) {
					if (i >= rs) {
						scope.w.write1(OpCodes.CONST_NIL);
						scope.top++;
					} else {
						rhs.get(i).visit(this, scope);
					}
					move(lhs.get(i), scope);
				}
			} else {
				for (int i = 0; i < ls; i++) {
					rhs.get(i).visit(this, scope);
					move(lhs.get(i), scope);
				}
			}
		}
		return null;
	}

	@Override
	public Void visitLocalFunction(LocalFunction value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitFunctionStatement(FunctionStatement value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitFunctionName(FunctionName value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitFunctionBody(FunctionBody value, Scope scope) {
		Scope local = scope.newScope("local");
		ParameterList params = value.parameterList;
		if (params.varargs)
			throw new IllegalStateException("NYI");
		for (String s : params.nameList)
			local.registerLocal(s);
		value.body.visit(this, local);

		LuaChunk chunk = local.build();
		chunk.paramCount = params.nameList.size();
//		scope.pool.add(chunk);
		return null;
	}

	@Override
	public Void visitParameterList(ParameterList value, Scope scope) {
		throw new IllegalStateException("NYI");
	}

	@Override
	public Void visitBlock(Block value, Scope scope) {
		for (Statement statement : value.statements)
			statement.visit(this, scope);
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
			case UnaryOp.OPR_MINUS:
				return OpCodes.NEG;
			case UnaryOp.OPR_NOT:
				return OpCodes.NOT;
			case UnaryOp.OPR_LEN:
				return OpCodes.LEN;
			default:
				throw new IllegalStateException("Unsupported: " + code);
		}
	}

	@Override
	public Void visitBinaryOp(BinaryOp value, Scope scope) {
		value.left.visit(this, scope);
		value.right.visit(this, scope);
		scope.w.write1(binaryOpCode(value.op));
		return null;
	}

	private static int binaryOpCode(int code) {
		switch (code) {
			case BinaryOp.OPR_ADD:
				return OpCodes.ADD;
			case BinaryOp.OPR_SUB:
				return OpCodes.SUB;
			case BinaryOp.OPR_MUL:
				return OpCodes.MUL;
			case BinaryOp.OPR_DIV:
				return OpCodes.DIV;
			default:
				throw new IllegalStateException("Unsupported: " + code);
		}
	}

	@Override
	public Void visitLiteral(Literal value, Scope scope) {
		ByteCodeWriter w = scope.w;
		if (value.type == Tokens.NAMESPACE) {
			String name = (String) value.value;
			Scope current = scope;
			while (current != null) {
				int index = current.locals.add(name);
				if (index != -1) {
					// TODO: 18/06/2017 Upvalues should be added here.
					if (current != scope)
						throw new IllegalStateException("Upvalues aren't supported: " + name);
					w.write1(OpCodes.CONST1, index);
					scope.top++;
					return null;
				}
				current = current.previous;
			}
			int index = scope.pool.add(name);
			w.write1(OpCodes.CONST1, index);
			w.write1(OpCodes.GET_GLOBAL);
			scope.top++;
		} else {
			// Load the literal
			int index = scope.pool.add(value.value);
			if (index < 256) {
				scope.w.write1(OpCodes.CONST1, index);
			} else {
				scope.w.write2(OpCodes.CONST2, index);
			}
			scope.top++;
		}
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

	private static final File ROOT = new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\main\\resources");

	private static LuaChunk nom(String name) throws IOException {
		LavaLexer lexer = new LavaLexer(new File(ROOT, name));
//		LavaLexer lexer = new LavaLexer(new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\test\\resources\\SyntaxTest10.lua"));
		LavaParser parser = new LavaParser(lexer);

		long start = System.nanoTime();
		Block chunk = parser.chunk();
		long end = System.nanoTime();
		long parserTime = end - start;
		System.out.println("AST: " + parserTime);

		LavaEmitter emitter = new LavaEmitter();
		Scope scope = new Scope(name);
		start = System.nanoTime();
		chunk.visit(emitter, scope);
		end = System.nanoTime();
		long visitorTime = end - start;
		System.out.println("Visitor: " + visitorTime);

		start = System.nanoTime();
		LuaChunk emitted = scope.build();
		end = System.nanoTime();
		long emitterTime = end - start;
		System.out.println("Emitter: " + emitterTime);
		System.out.println("Total: " + (parserTime + visitorTime + emitterTime));
		return emitted;
	}
}
