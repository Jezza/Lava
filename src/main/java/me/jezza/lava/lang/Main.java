package me.jezza.lava.lang;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import me.jezza.lava.lang.ast.ParseTree.Block;
import me.jezza.lava.lang.interfaces.Lexer;
import me.jezza.lava.runtime.Interpreter.LuaChunk;

/**
 * @author Jezza
 */
public final class Main {
	private Main() {
		throw new IllegalStateException();
	}

	public static void main(String[] args) throws Throwable {
		LuaChunk chunk = nom(new File(ROOT, "test.lua"));
//		Interpreter.test(chunk);
//		runAll();
//		run("constructs.lua");
	}

	private static final String base = "C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\test\\resources";

	private static final void run(String name) {
		try {
			nom(Paths.get(base, name).toFile());
		} catch (Throwable e) {
			throw new IllegalStateException("File: " + name, e);
		}
	}

	private static final void runAll() throws IOException {
		long count = Files.list(Paths.get(base))
				.filter(child -> Files.isRegularFile(child) && child.getFileName().toString().endsWith(".lua"))
				.mapToLong(child -> {
					try {
						File file = child.toFile();
						long start = System.nanoTime();
						long length = nomTime(file);
						long time = System.nanoTime() - start;
						System.out.println("D: " + time + ", length: " + length);
						return time;
					} catch (Throwable e) {
						throw new IllegalStateException("File: " + child, e);
					}
				}).sum();
		System.out.println("Time: " + count);

	}

	private static final File ROOT = new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\main\\resources");

	public static File resolve(String name) {
		return new File(ROOT, name);
	}

	private static LuaChunk nom(File data) throws IOException {
//		Lexer lexer = new PrintLexer(new LavaLexer(data));
		Lexer lexer = new LavaLexer(data);
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

	private static long nomTime(File data) throws IOException {
		Lexer lexer = new LavaLexer(data);
		LavaParser parser = new LavaParser(lexer);
		Block chunk = parser.chunk();
		String text = ASTPrinter.print(chunk);
		return text.length();
	}

	private static final class PrintLexer implements Lexer {
		private final Lexer lexer;

		public PrintLexer(Lexer lexer) {
			this.lexer = lexer;
		}

		@Override
		public Token next() throws IOException {
			Token next = lexer.next();
			System.out.println(next);
			return next;
		}
	}
}
