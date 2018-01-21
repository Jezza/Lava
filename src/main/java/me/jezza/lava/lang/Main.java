package me.jezza.lava.lang;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import me.jezza.lava.lang.ast.ParseTree.Block;
import me.jezza.lava.lang.interfaces.Lexer;
import me.jezza.lava.runtime.Interpreter;
import me.jezza.lava.runtime.Interpreter.LuaChunk;

/**
 * @author Jezza
 */
public final class Main {
	private Main() {
		throw new IllegalStateException();
	}

	private static final String base = "C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\test\\resources";

	public static void main(String[] args) throws Throwable {
//		long basic = run(Paths.get(base).resolve("accept-basic"));
//		long libs = run(Paths.get(base).resolve("libs"));
//		long libssub = run(Paths.get(base).resolve("libs").resolve("P1"));
//		long speed = run(Paths.get(base).resolve("speed"));
//		long root = run(Paths.get(base));
//		run("constructs.lua");
//		System.out.println(root);
//		System.out.println(basic);
//		System.out.println(libs);
//		System.out.println(libssub);
//		System.out.println(speed);

		LuaChunk chunk = nom(new File(ROOT, "lang.lua"));
		Interpreter.test(chunk);
	}

	private static void run(String name) {
		try {
			nom(Paths.get(base, name).toFile());
		} catch (Throwable e) {
			throw new IllegalStateException("File: " + name, e);
		}
	}

	private static long run(Path root) throws IOException {
		return Files.list(root)
				.filter(child -> Files.isRegularFile(child) && child.getFileName().toString().endsWith(".lua"))
				.mapToLong(child -> {
					try {
						return nomTime(child.toFile());
					} catch (Throwable e) {
						throw new IllegalStateException("File: " + child, e);
					}
				}).sum();
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
//		Scope scope = new Scope(data.getName());
//		start = System.nanoTime();
//		chunk.visit(emitter, scope);
//		end = System.nanoTime();
//		long visitorTime = end - start;
//		System.out.println("Visitor: " + visitorTime);


		start = System.nanoTime();
		LuaChunk emitted = LavaEmitter.emit(data.getName(), chunk);
		end = System.nanoTime();
		long emitterTime = end - start;
		System.out.println("Emitter: " + emitterTime);
		System.out.println("Total: " + (parserTime + emitterTime));
		return emitted;
	}

	private static long nomTime(File data) throws IOException {
		long start = System.nanoTime();
		Lexer lexer = new LavaLexer(data);
		LavaParser parser = new LavaParser(lexer);
		Block chunk = parser.chunk();
		String text = ASTPrinter.print(chunk);
		long end = System.nanoTime();
		System.out.println(text);
		return end - start;
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
