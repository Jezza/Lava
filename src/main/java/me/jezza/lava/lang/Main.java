package me.jezza.lava.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import me.jezza.lava.lang.ParseTree.FunctionBody;
import me.jezza.lava.lang.interfaces.Lexer;
import me.jezza.lava.lang.model.LavaLexer;
import me.jezza.lava.lang.model.LavaParser;
import me.jezza.lava.runtime.Interpreter;
import me.jezza.lava.runtime.Interpreter.LuaChunk;

/**
 * @author Jezza
 */
public final class Main {
	private Main() {
		throw new IllegalStateException();
	}

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

		LuaChunk chunk = nom("lang.lua", resolve("/lang.lua"));
		Interpreter.testChunk(chunk, 1);
	}

	public static InputStream resolve(String name) {
		return Main.class.getResourceAsStream(name);
	}

	public static String readResource(String name) {
		var resource = resolve(name);
		if (resource == null) {
			return null;
		}
		try (resource) {
			return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private static void run(String name) {
		try {
//			nom(Paths.get(base, name).toFile());
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
				})
				.sum();
	}

	private static LuaChunk nom(String name, InputStream data) throws IOException {
		Lexer lexer = new LavaLexer(data);
		LavaParser parser = new LavaParser(lexer);

		long start = System.nanoTime();
		FunctionBody block = parser.chunk();
		block.name = name;
		long end = System.nanoTime();
		long parserTime = end - start;
		System.out.println("AST: " + parserTime);

		System.out.println(ASTPrinter.print(block));
		SemanticAnalysis.run(block);
		System.out.println(ASTPrinter.print(block));

		start = System.nanoTime();
		LuaChunk emitted = LavaEmitter.emit(name, block);
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
		FunctionBody chunk = parser.chunk();
		chunk.name = data.getName();
		String text = ASTPrinter.print(chunk);
		long end = System.nanoTime();
		System.out.println(text);
		return end - start;
	}
}
