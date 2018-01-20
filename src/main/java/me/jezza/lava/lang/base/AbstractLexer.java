package me.jezza.lava.lang.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads text character by character from a "character-stream" into a buffer.
 * The buffer size may be specified, or the default size will be used.
 * The default is generally more than enough for most use cases.
 * <p>
 * This class uses a chunk buffer.
 * On read, a block is read from the input, and stored.
 * Each call to {@link #advance()} steps through this buffer.
 * Once it reaches the end of the buffer, the next chunk is retrieved.
 * <p>
 * The {@link #peek()} method can be used to look at the current character without consuming it.
 * <p>
 * The constructors of this class have been overloaded to support the most common forms of characters streams.
 * <p>
 * The initial use case of this class was for lexers.
 * The pattern would be something akin to "extend it, and provide the basic constructors that are needed".
 * Once that was done, any external interfaces could be implemented, and this classes functionality could be used.
 * An example would be a Lexer interface that contains a single method:
 * <pre>
 *    interface Lexer {
 * 		Token next() throws IOException;
 *    }
 * </pre>
 * A basic implementation would be to extend this class, implement that interface,
 * and using the {@link #advance()} and {@link #peek()} methods on this class, provide a lexer/tokeniser.
 *
 * @author Jezza
 */
public abstract class AbstractLexer {
	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private static final int START_COLUMN = 0;
	private static final int START_ROW = 0;

	private static final int MODE_KILL = 3;
	private static final int MODE_DRAIN = 2;
	private static final int MODE_READ = 1;
	private static final int MODE_UNINITIALISED = 0;

	protected static final int EOS = -1;

	/**
	 * The input from which to read from.
	 * <p>
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

	private int index;
	private int limit;

	/**
	 * Creates a new lexer that steps through the given string.
	 *
	 * @param input - String providing the character stream.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(String input) {
		this(new StringReader(input), DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new lexer that steps through the given string.
	 *
	 * @param input      - The string providing the character stream.
	 * @param bufferSize - The internal buffer size.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(String input, int bufferSize) {
		this(new StringReader(input), bufferSize);
	}

	/**
	 * Creates a new lexer using the file with the
	 * system default charset.
	 *
	 * @param file - The file to read.
	 * @throws FileNotFoundException    - If the file does not exist,
	 *                                  is a directory rather than a regular file,
	 *                                  or for some other reason cannot be opened for
	 *                                  reading.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(File file) throws FileNotFoundException {
		this(new FileReader(file), DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new lexer using the file with the
	 * specified charset.
	 *
	 * @param file    - The file to read.
	 * @param charset - The charset that will be used when reading the file.
	 * @throws FileNotFoundException    - If the file does not exist,
	 *                                  is a directory rather than a regular file,
	 *                                  or for some other reason cannot be opened for
	 *                                  reading.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(File file, Charset charset) throws FileNotFoundException {
		this(new FileInputStream(file), charset, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new lexer using the file with the
	 * system default charset, and the given
	 * buffer size.
	 *
	 * @param file       - The file to read.
	 * @param bufferSize - The internal buffer size.
	 * @throws FileNotFoundException    - If the file does not exist,
	 *                                  is a directory rather than a regular file,
	 *                                  or for some other reason cannot be opened for
	 *                                  reading.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(File file, int bufferSize) throws FileNotFoundException {
		this(new FileReader(file), bufferSize);
	}

	/**
	 * Creates a new lexer using the file with the
	 * specified charset, and the given buffer size.
	 *
	 * @param file       - The file to read.
	 * @param charset    - The charset that will be used when reading the file.
	 * @param bufferSize - The internal buffer size.
	 * @throws FileNotFoundException    - If the file does not exist,
	 *                                  is a directory rather than a regular file,
	 *                                  or for some other reason cannot be opened for
	 *                                  reading.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(File file, Charset charset, int bufferSize) throws FileNotFoundException {
		this(new FileInputStream(file), charset, bufferSize);
	}

	/**
	 * Creates a new lexer using the file with the
	 * system default charset.
	 *
	 * @param path - The path to read.
	 * @throws IOException              - If an I/O error occurs
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(Path path) throws IOException {
		this(Files.newInputStream(path), DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new lexer using the file with the
	 * specified charset.
	 *
	 * @param path    - The path to read.
	 * @param charset - The charset that will be used when reading the file.
	 * @throws IOException              - If an I/O error occurs
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(Path path, Charset charset) throws IOException {
		this(Files.newInputStream(path), charset, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new lexer using the file with the
	 * system default charset, and the given
	 * buffer size.
	 *
	 * @param path       - The path to read.
	 * @param bufferSize - The internal buffer size.
	 * @throws IOException              - If an I/O error occurs
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(Path path, int bufferSize) throws IOException {
		this(Files.newInputStream(path), bufferSize);
	}

	/**
	 * Creates a new lexer using the file with the
	 * specified charset, and the given buffer size.
	 *
	 * @param path       - The path to read.
	 * @param charset    - The charset that will be used when reading the file.
	 * @param bufferSize - The internal buffer size.
	 * @throws IOException              - If an I/O error occurs
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(Path path, Charset charset, int bufferSize) throws IOException {
		this(Files.newInputStream(path), charset, bufferSize);
	}

	/**
	 * Creates a new lexer using the InputStream
	 * with the system default charset.
	 *
	 * @param in - The InputStream from which to read.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(InputStream in) {
		this(new InputStreamReader(in), DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new lexer using the InputStream
	 * with the specified charset.
	 *
	 * @param in      - The InputStream from which to read.
	 * @param charset - The charset that will be used when reading the InputStream.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(InputStream in, Charset charset) {
		this(new InputStreamReader(in, charset), DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new lexer using the InputStream
	 * with the system default charset, and the given
	 * buffer size.
	 *
	 * @param in         - The InputStream from which to read.
	 * @param bufferSize - The internal buffer size.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(InputStream in, int bufferSize) {
		this(new InputStreamReader(in), bufferSize);
	}

	/**
	 * Creates a new lexer using the InputStream
	 * with the specified charset, and the given
	 * buffer size.
	 *
	 * @param in         - The InputStream from which to read.
	 * @param charset    - The charset that will be used when reading the InputStream.
	 * @param bufferSize - The internal buffer size.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(InputStream in, Charset charset, int bufferSize) {
		this(new InputStreamReader(in, charset), bufferSize);
	}

	/**
	 * Creates a new lexer using the Reader, using
	 * the given buffer size.
	 *
	 * @param in - The Reader from which to read.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(Reader in) {
		this(in, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new lexer using the Reader, using
	 * the given buffer size.
	 *
	 * @param in         - The Reader from which to read.
	 * @param bufferSize - The internal buffer size.
	 * @throws NullPointerException     - If the input is null
	 * @throws IllegalArgumentException - If the buffer size is not positive.
	 */
	protected AbstractLexer(Reader in, int bufferSize) {
		if (in == null)
			throw new NullPointerException("Input cannot be null.");
		if (bufferSize < 1)
			throw new IllegalArgumentException("Buffer size must be > 0");
		this.in = in;
		this.buffer = new char[bufferSize];
		mode = MODE_UNINITIALISED;
		pos = new int[]{START_ROW, START_COLUMN};
		index = 0;
		limit = -1;
	}

	private boolean nextChunk() throws IOException {
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
		return false;
	}

	protected final int advance() throws IOException {
		if (mode == MODE_UNINITIALISED) {
			mode = MODE_READ;
			if (nextChunk())
				return EOS;
		} else if (mode == MODE_KILL)
			return EOS;
		int c = buffer[index++];
		if (limit != -1 && index >= limit)
			mode = MODE_KILL;
		else if (in != null && buffer.length - index == 0)
			nextChunk();
		if (c == '\n') {
			pos[1] = START_COLUMN;
			pos[0]++;
		} else if (c != '\r') {
			pos[1]++;
		}
		return c;
	}

	protected final int peek() throws IOException {
		if (mode == MODE_KILL) {
			return EOS;
		} else if (mode == MODE_UNINITIALISED) {
			mode = MODE_READ;
			nextChunk();
		}
		return buffer[index];
	}
}