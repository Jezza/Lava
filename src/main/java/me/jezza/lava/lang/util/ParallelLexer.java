package me.jezza.lava.lang.util;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

import me.jezza.lava.Times;
import me.jezza.lava.lang.Token;
import me.jezza.lava.lang.Tokens;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public final class ParallelLexer implements Lexer, Runnable {
	private final Lexer delegate;

	private BlockingDeque<Object> tokens;
	private Token end;

	private ParallelLexer(Lexer delegate) {
		this.delegate = delegate;

		tokens = new LinkedBlockingDeque<>();
		CompletableFuture.runAsync(this);
	}

	@Override
	public void run() {
		while (true) {
			try {
				Token t = delegate.next();
				tokens.putLast(t);
				if (t.type == Tokens.EOS) {
					System.out.println("Done: " + tokens.size());
					return;
				}
			} catch (Exception e) {
				tokens.offerLast(e);
			}
		}
	}

	private static final Times NEXT = new Times("PARALLEL_NEXT", 256);

	@Override
	public Token next() throws IOException {
		long start = System.nanoTime();
		try {
			if (end != null)
				return this.end;
			Object obj = null;
			try {
				Token t = (Token) (obj = tokens.takeFirst());
				if (t.type == Tokens.EOS)
					end = t;
				return t;
			} catch (ClassCastException e) {
				if (obj instanceof IOException)
					throw (IOException) obj;
				if (obj instanceof Exception)
					throw new IOException((Exception) obj);
				throw new IOException(e);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		} finally {
			long end = System.nanoTime();
			NEXT.add(end - start);
		}
	}

	public static Lexer of(Lexer lexer) {
		return new ParallelLexer(lexer);
	}
}