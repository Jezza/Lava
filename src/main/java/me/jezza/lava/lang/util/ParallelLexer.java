package me.jezza.lava.lang.util;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import me.jezza.lava.lang.model.Token;
import me.jezza.lava.lang.model.Tokens;
import me.jezza.lava.lang.interfaces.Lexer;

/**
 * @author Jezza
 */
public final class ParallelLexer implements Lexer, Runnable {
	private static final int CONCURRENT = 0;
	private static final int SWAPPING = 1;
	private static final int FREE = 2;

	private final Lexer delegate;

	private BlockingQueue<Object> blockingTokens;
	private Token[] tokens;
	private int index;
	private volatile int mode;
	private CountDownLatch swapSignal;
	private Token end;

	private ParallelLexer(Lexer delegate) {
		this.delegate = delegate;
		blockingTokens = new LinkedBlockingDeque<>();
		swapSignal = new CountDownLatch(1);
		mode = CONCURRENT;
		index = 0;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Token t = delegate.next();
				blockingTokens.offer(t);
				if (t.type == Tokens.EOS) {
					mode = SWAPPING;
					tokens = blockingTokens.toArray(new Token[0]);
					mode = FREE;
					swapSignal.countDown();
					System.out.println("Done with " + tokens.length);
					return;
				}
			} catch (Exception e) {
				blockingTokens.offer(e);
				break;
			}
		}
	}

	private Object takeNext() throws IOException {
		if (mode != FREE) {
			try {
				if (mode == CONCURRENT) {
					return blockingTokens.take();
				}
				swapSignal.await();
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
		return tokens[index++];
	}

	@Override
	public Token next() throws IOException {
		if (end != null) {
			return this.end;
		}
		Object obj = null;
		try {
			Token t = (Token) (obj = takeNext());
			if (t.type == Tokens.EOS) {
				end = t;
			}
			return t;
		} catch (ClassCastException e) {
			if (obj instanceof IOException) {
				throw (IOException) obj;
			}
			if (obj instanceof Exception) {
				throw new IOException((Exception) obj);
			}
			throw new IOException(e);
		}
	}

	public static Lexer of(Lexer lexer) {
		ParallelLexer parallel = new ParallelLexer(lexer);
		CompletableFuture.runAsync(parallel);
		return parallel;
	}
}