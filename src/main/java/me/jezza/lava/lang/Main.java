package me.jezza.lava.lang;

import java.io.File;
import java.io.IOException;

import me.jezza.lava.lang.ast.ParseTree.Block;
import me.jezza.lava.runtime.Interpreter.LuaChunk;

/**
 * @author Jezza
 */
public final class Main {
	private Main() {
		throw new IllegalStateException();
	}

	public static void main(String[] args) throws Throwable {
		System.out.println("DS");
		LuaChunk chunk = nom("test.lua");
//		Interpreter.test(chunk);
	}

	private static final File ROOT = new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\main\\resources");

	public static File resolve(String name) {
		return new File(ROOT, name);
	}

	private static LuaChunk nom(String name) throws IOException {
		LavaLexer lexer = new LavaLexer(new File(ROOT, name));
//		LavaLexer lexer = new LavaLexer(new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\test\\resources\\SyntaxTest10.lua"));
		LavaParser parser = new LavaParser(lexer);

		long start = System.nanoTime();
		Block chunk = parser.chunk();
		long end = System.nanoTime();
		long parserTime = end - start;
		System.out.println("AST: " + parserTime);

		String text = ASTPrinter.print(chunk);
		System.out.println(text);

//		LoweringPhase.run(chunk);

//		LavaEmitter emitter = new LavaEmitter();
//		Scope scope = new Scope(name);
//		start = System.nanoTime();
//		chunk.visit(emitter, scope);
//		end = System.nanoTime();
//		long visitorTime = end - start;
//		System.out.println("Visitor: " + visitorTime);  + visitorTime


//		start = System.nanoTime();
//		LuaChunk emitted = LavaEmitter.emit(name, chunk);
//		end = System.nanoTime();
//		long emitterTime = end - start;
//		System.out.println("Emitter: " + emitterTime);
//		System.out.println("Total: " + (parserTime + emitterTime));
//		return emitted;
		return null;
	}
}
