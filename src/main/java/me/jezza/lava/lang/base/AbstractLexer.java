package me.jezza.lava.lang.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

/**
 * A very simple extension on the Reader classes.
 * Provides a good base for lexers, and the like, by unifying various input formats. (Files, Strings, InputStreams, etc)
 * And it does so by adding so little overhead.
 *
 * @author Jezza
 */
public abstract class AbstractLexer {
	private static final int DEFAULT_BUFFER_SIZE = 2048;

	private static final int START_COLUMN = 1;
	private static final int START_ROW = 1;

	private static final int MODE_KILL = 3;
	private static final int MODE_DRAIN = 2;
	private static final int MODE_READ = 1;
	private static final int MODE_UNINITIALISED = 0;

	private static final int EOS = -1;

	/**
	 * The input from which to read from.
	 *
	 * While not actually final, this should be treated as such.
	 * The only reason this isn't final is because we need a nice, fast, and clean way of knowing when we closed it.
	 * So, null is used as a "There's no more input" internally, beside the mode state.
	 */
	private /*final*/ Reader in;

	/**
	 * The internal buffer with a capacity as defined by the numerous constructors,
	 * or if no such constructor was used, {@link #DEFAULT_BUFFER_SIZE}.
	 */
	private final char[] buffer;

	protected final int[] pos;

	private int mode;

	private int index = 0;
	private int limit = -1;

	protected AbstractLexer(final String input) {
		this(new StringReader(input), DEFAULT_BUFFER_SIZE);
	}

	protected AbstractLexer(final String input, int bufferSize) {
		this(new StringReader(input), bufferSize);
	}

	protected AbstractLexer(final File file) throws FileNotFoundException {
		this(new FileReader(file), DEFAULT_BUFFER_SIZE);
	}

	protected AbstractLexer(final File file, int bufferSize) throws FileNotFoundException {
		this(new FileReader(file), bufferSize);
	}

	protected AbstractLexer(final InputStream in) {
		this(new InputStreamReader(in), DEFAULT_BUFFER_SIZE);
	}

	protected AbstractLexer(final InputStream in, int bufferSize) {
		this(new InputStreamReader(in), bufferSize);
	}

	protected AbstractLexer(final Reader in) {
		this(in, DEFAULT_BUFFER_SIZE);
	}

	protected AbstractLexer(final Reader in, int length) {
		if (in == null)
			throw new NullPointerException("Input cannot be null.");
		if (length < 1)
			throw new IllegalArgumentException("Buffer size must be > 0");
		this.in = in;
		this.buffer = new char[length];
		mode = MODE_UNINITIALISED;
		pos = new int[]{START_COLUMN, START_ROW};
	}

//	private static final Times CHUNK = new Times("nextChunk", 1024);

	private boolean nextChunk() throws IOException {
//		final long start = System.nanoTime();
		int count = in.read(buffer);
		if (count == EOS) {
			in.close();
			in = null;
			mode = MODE_KILL;
			return true;
		} else if (count < buffer.length) {
			index = 0;
			limit = count;
			in.close();
			in = null;
			mode = MODE_DRAIN;
		} else {
			index = 0;
		}
//		CHUNK.add(System.nanoTime() - start);
		return false;
	}

//	private static final Times ADVANCE = new Times("advance", 4096);

	protected final int advance() throws IOException {
//		final long start = System.nanoTime();
		if (mode == MODE_UNINITIALISED) {
			mode = MODE_READ;
			if (nextChunk())
				return EOS;
		} else if (mode == MODE_KILL)
			return EOS;
		final int c = buffer[index++];
		if (limit != -1 && index >= limit)
			mode = MODE_KILL;
		else if (in != null && buffer.length - index == 0)
			nextChunk();
		if (c == '\n') {
			pos[0] = START_COLUMN;
			pos[1]++;
		} else if (c != '\r') {
			pos[0]++;
		}
//		ADVANCE.add(System.nanoTime() - start);
		return c;
	}

	protected final String inputBuffer() {
		return Arrays.toString(buffer);
	}

	protected final int peek() throws IOException {
		if (mode == MODE_KILL)
			return EOS;
		else if (mode == MODE_UNINITIALISED)
			nextChunk();
		return buffer[index];
	}

//	protected final int peek(int offset) throws IOException {
//		if (mode == MODE_KILL)
//			return -1;
//		else if (mode == MODE_UNINITIALISED)
//			nextChunk();
//		if (offset > length)
//			throw new IllegalStateException("Can't lookahead greater than the buffer size");
////		int position = buffer.position();
//		int result = buffer.get(buffer.position() + offset);
////		buffer.position(position);
//		return result;
//	}

	static class Lex extends AbstractLexer {
		protected Lex(String input, int bufferSize) {
			super(input, bufferSize);
		}
	}

	public static void main(String[] args) throws IOException {
		final Lex lex = new Lex("0126456498489414864180648764784df78g4df7g410d7g410 754g017 sdf10g 7sd10 g74s56 7d0fg176sdf01g7 sdfg10 7dfg0 1d7sg 01d70g1 6sd01 g6df0g4178sdg68s40 8f47s 6f74g 086sd074g 86sdf40g 8s6dg48s6d40g 68sd40g 86sd", 1024);
		int c;
		final long start = System.nanoTime();
		int count = 0;
		while ((c = lex.advance()) != EOS) {
			count++;
		}
		final long end = System.nanoTime();
		System.out.println(end - start);
		System.out.println("Count: " + count);
	}
}