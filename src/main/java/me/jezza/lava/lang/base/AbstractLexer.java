package me.jezza.lava.lang.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * A very basic abstract lexer that provides a very good base to use.
 *
 * @author Jezza
 */
public abstract class AbstractLexer {
	private static final int DEFAULT_BUFFER_SIZE = 2048;

	private static final int START_COLUMN = 1;
	private static final int START_ROW = 1;

	private static final int MODE_IMMEDIATE_KILL = 3;
	private static final int MODE_DRAIN = 2;
	private static final int MODE_READ = 1;
	private static final int MODE_UNINITIALISED = 0;

	private static final int EOS = -1;

	private Reader in;

	private final CharBuffer buffer;

	private final int length;

	protected final int[] pos;

	private int mode;

	protected AbstractLexer(final String input) {
		this(new StringReader(input), DEFAULT_BUFFER_SIZE);
	}

	protected AbstractLexer(final String input, int bufferSize) {
		this(new StringReader(input), bufferSize);
	}

	protected AbstractLexer(final File file) throws FileNotFoundException {
		this(new FileReader(file), DEFAULT_BUFFER_SIZE, ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE).asCharBuffer());
	}

	protected AbstractLexer(final File file, int bufferSize) throws FileNotFoundException {
		this(new FileReader(file), bufferSize, ByteBuffer.allocateDirect(bufferSize).asCharBuffer());
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

	protected AbstractLexer(final Reader in, int bufferSize) {
		this(in, bufferSize, CharBuffer.allocate(bufferSize));
	}

	protected AbstractLexer(final Reader in, int bufferSize, CharBuffer buffer) {
		if (in == null)
			throw new NullPointerException("Input cannot be null.");
		if (buffer == null)
			throw new NullPointerException("Buffer cannot be null.");
		if (bufferSize < 1)
			throw new IllegalArgumentException("Buffer size must be > 0");
		this.in = in;
		this.buffer = buffer;
		length = bufferSize;
		mode = MODE_UNINITIALISED;
		pos = new int[]{START_COLUMN, START_ROW};
	}

//	private static final Times CHUNK = new Times("nextChunk", 1);

	private void nextChunk() throws IOException {
//		long start = System.nanoTime();
		int count = in.read(buffer);
		if (count == EOS) {
			buffer.limit(buffer.position() + length);
			in.close();
			in = null;
			mode = MODE_IMMEDIATE_KILL;
			buffer.position(0);
		} else if (buffer.remaining() > 0) {
			buffer.position(0);
			buffer.limit(count);
			in.close();
			in = null;
			mode = MODE_DRAIN;
		} else {
			buffer.position(0);
		}
//		CHUNK.add(System.nanoTime() - start);
	}

//	private static final Times ADVANCE = new Times("advance", 4096);

	protected final int advance() throws IOException {
//		long start = System.nanoTime();
		if (mode == MODE_UNINITIALISED) {
			mode = MODE_READ;
			nextChunk();
		} else if (mode == MODE_IMMEDIATE_KILL) {
			return -1;
		}
		final int c = buffer.get();
		final int remaining = buffer.remaining();
		if (remaining == 0) {
			if (in != null) {
				buffer.position(0);
				nextChunk();
			} else {
				mode = MODE_IMMEDIATE_KILL;
			}
		}
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
		return buffer.toString();
	}

	protected final int peek() throws IOException {
		if (mode == MODE_IMMEDIATE_KILL)
			return -1;
		else if (mode == MODE_UNINITIALISED)
			nextChunk();
		return buffer.get(buffer.position());
	}

//	protected final int peek(int offset) throws IOException {
//		if (mode == MODE_IMMEDIATE_KILL)
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
}