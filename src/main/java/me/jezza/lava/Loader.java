/**
 * Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject
 * to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package me.jezza.lava;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads Lua 5.1 binary chunks.
 * This loader is restricted to loading Lua 5.1 binary chunks where:
 * <ul>
 * <li><code>LUAC_VERSION</code> is <code>0x51</code>.</li>
 * <li><code>int</code> is 32 bits.</li>
 * <li><code>size_t</code> is 32 bits.</li>
 * <li><code>Instruction</code> is 32 bits (this is a type defined in
 * the PUC-Rio Lua).</li>
 * <li><code>lua_Number</code> is an IEEE 754 64-bit double.  Suitable
 * for passing to {@link java.lang.Double#longBitsToDouble}.</li>
 * <li>endianness does not matter (the loader swabs as appropriate).</li>
 * </ul>
 * Any Lua chunk compiled by a stock Lua 5.1 running on a 32-bit Windows
 * PC or at 32-bit OS X machine should be fine.
 */
public final class Loader {
	/**
	 * Whether integers in the binary chunk are stored big-endian or
	 * little-endian.  Recall that the number 0x12345678 is stored: 0x12
	 * 0x34 0x56 0x78 in big-endian format; and, 0x78 0x56 0x34 0x12 in
	 * little-endian format.
	 */
	private boolean bigendian;
	private InputStream in;
	private String name;

	// auxiliary for reading ints/numbers
	private byte[] intbuf = new byte[4];
	private byte[] longbuf = new byte[8];

	/**
	 * A new chunk loader.  The <code>InputStream</code> must be
	 * positioned at the beginning of the <code>LUA_SIGNATURE</code> that
	 * marks the beginning of a Lua binary chunk.
	 *
	 * @param in   The binary stream from which the chunk is read.
	 * @param name The name of the chunk.
	 */
	Loader(InputStream in, String name) {
		if (in == null)
			throw new NullPointerException();
		this.in = in;
		// The name is treated slightly.  See lundump.c in the PUC-Rio source for details.
		this.name = name.charAt(0) == '@' || name.charAt(0) == '=' ? name.substring(1) : name;
	}

	/**
	 * Loads (undumps) a dumped binary chunk.
	 *
	 * @throws IOException if chunk is malformed or unacceptable.
	 */
	Proto undump() throws IOException {
		header();
		return readFunction(null);
	}


	/**
	 * Primitive reader for undumping.
	 * Reads exactly enough bytes from <code>this.in</code> to fill the
	 * array <code>b</code>.  If there aren't enough to fill
	 * <code>b</code> then an exception is thrown.  Similar to
	 * <code>LoadBlock</code> from PUC-Rio's <code>lundump.c</code>.
	 *
	 * @param b byte array to fill.
	 * @throws EOFException when the stream is exhausted too early.
	 * @throws IOException  when the underlying stream does.
	 */
	private void read(byte[] b) throws IOException {
		if (in.read(b) != b.length)
			throw new EOFException();
	}

	/**
	 * Undumps a byte as an 8 bit unsigned number.
	 * Note: returns an int to accommodate the range.
	 *
	 * @return - The unsigned byte that was read.
	 * @throws IOException - If End of File was unexpectedly reached.
	 */
	private int readUByte() throws IOException {
		int c = in.read();
		if (c == -1)
			throw new EOFException();
		return c & 0xFF;  // paranoia
	}

	/**
	 * Undumps an int.  This method swabs accordingly.
	 * size_t and Instruction need swabbing too, but the code
	 * simply uses this method to load size_t and Instruction.
	 */
	private int readInt() throws IOException {
		// :int:size  Here we assume an int is 4 bytes.
		read(intbuf);
		// Caution: byte is signed so "&0xff" converts to unsigned value.
		if (bigendian)
			return ((intbuf[0] & 0xff) << 24) | ((intbuf[1] & 0xff) << 16) | ((intbuf[2] & 0xff) << 8) | (intbuf[3] & 0xff);
		return ((intbuf[3] & 0xff) << 24) | ((intbuf[2] & 0xff) << 16) | ((intbuf[1] & 0xff) << 8) | (intbuf[0] & 0xff);
	}

	/**
	 * Undumps the code for a <code>Proto</code>.
	 * The code is an array of VM instructions.
	 */
	private int[] readCode() throws IOException {
		int n = readInt();
		int[] code = new int[n];
		// :Instruction:size  Here we assume that a dumped Instruction is the same size as a dumped int.
		for (int i = 0; i < n; ++i)
			code[i] = readInt();
		return code;
	}

	/**
	 * Undumps the constant array contained inside a <code>Proto</code>
	 * object.  First half of <code>LoadConstants</code>, see
	 * <code>proto</code> for the second half of
	 * <code>LoadConstants</code>.
	 */
	private Slot[] constant() throws IOException {
		int n = readInt();
		Slot[] k = new Slot[n];

		// Load each constant one by one.  We use the following values for
		// the Lua tagtypes (taken from <code>lua.h</code> from the PUC-Rio
		// Lua 5.1 distribution):
		// LUA_TNIL         0
		// LUA_TBOOLEAN     1
		// LUA_TNUMBER      3
		// LUA_TSTRING      4
		// All other tagtypes are invalid

		// :todo: Currently a new Slot is created for each constant.
		// Consider a space optimisation whereby identical constants have
		// the same Slot.  Constants are pooled per function anyway (so a
		// function never has 2 identical constants), so would have to work
		// across functions.  The easy cases of nil, true, false, might be
		// worth doing since that doesn't require a global table.
		//
		for (int i = 0; i < n; ++i) {
			switch (readUByte()) {
				case 0: // LUA_TNIL
					k[i] = new Slot(Lua.NIL);
					break;

				case 1: // LUA_TBOOLEAN
					int b = readUByte();
					// assert b >= 0;
					if (b > 1)
						throw new IOException();
					k[i] = new Slot(b != 0);
					break;

				case 3: // LUA_TNUMBER
					k[i] = new Slot(number());
					break;

				case 4: // LUA_TSTRING
					k[i] = new Slot(readString());
					break;

				default:
					throw new IOException();
			}
		}

		return k;
	}

	/**
	 * Undumps the debug info for a <code>Proto</code>.
	 *
	 * @param proto The Proto instance to which debug info will be added.
	 */
	private void debug(Proto proto) throws IOException {
		// lineinfo
		int n = readInt();
		int[] lineinfo = new int[n];

		for (int i = 0; i < n; ++i)
			lineinfo[i] = readInt();

		// locvars
		n = readInt();
		LocVar[] locvar = new LocVar[n];
		for (int i = 0; i < n; ++i)
			locvar[i] = new LocVar(readString(), readInt(), readInt());

		// upvalue (names)
		n = readInt();
		String[] upvalue = new String[n];
		for (int i = 0; i < n; ++i)
			upvalue[i] = readString();

		proto.debug(lineinfo, locvar, upvalue);
	}

	/**
	 * Undumps a Proto object.  This is named 'function' after
	 * <code>LoadFunction</code> in PUC-Rio's <code>lundump.c</code>.
	 *
	 * @param parentSource Name of parent source "file".
	 * @throws IOException when binary is malformed.
	 */
	private Proto readFunction(String parentSource) throws IOException {
		String source = readString();
		if (source == null)
			source = parentSource;
		int linedefined = this.readInt();
		int lastlinedefined = this.readInt();
		int nups = this.readUByte();
		int numparams = this.readUByte();
		int varargByte = this.readUByte();
		// "is_vararg" is a 3-bit field, with the following bit meanings
		// (see "lobject.h"):
		// 1 - VARARG_HASARG
		// 2 - VARARG_ISVARARG
		// 4 - VARARG_NEEDSARG
		// Values 1 and 4 (bits 0 and 2) are only used for 5.0
		// compatibility.
		// HASARG indicates that a function was compiled in 5.0
		// compatibility mode and is declared to have ... in its parameter
		// list.
		// NEEDSARG indicates that a function was compiled in 5.0
		// compatibility mode and is declared to have ... in its parameter
		// list and does _not_ use the 5.1 style of vararg access (using ...
		// as an expression).  It is assumed to use 5.0 style vararg access
		// (the local 'arg' variable).  This is not supported in Jill.
		// ISVARARG indicates that a function has ... in its parameter list
		// (whether compiled in 5.0 compatibility mode or not).
		//
		// At runtime NEEDSARG changes the protocol for calling a vararg
		// function.  We don't support this, so we check that it is absent
		// here in the loader.
		//
		// That means that the legal values for this field ar 0,1,2,3.
		if (varargByte < 0 || varargByte > 3)
			throw new IOException();
		boolean vararg = 0 != varargByte;
		int maxStackSize = readUByte();
		int[] code = readCode();
		Slot[] constant = constant();
		Proto[] proto = proto(source);
		Proto newProto = new Proto(constant, code, proto, nups, numparams, vararg, maxStackSize);
		newProto.setSource(source);
		newProto.setLinedefined(linedefined);
		newProto.setLastlinedefined(lastlinedefined);

		debug(newProto);
		// :todo: call code verifier
		return newProto;
	}

	private static final int HEADER_SIZE = 12;

	/**
	 * A chunk header that is correct.  Except for the endian byte, at
	 * index 6, which is always overwritten with the one from the file,
	 * before comparison.  We cope with either endianness.
	 * Default access so that {@link Lua#load} can read the first entry.
	 * On no account should anyone except {@link #header} modify
	 * this array.
	 *
	 * TODO Look up the first byte. I don't think it's supposed to be an octal, but I could be wrong.
	 */
	static final byte[] HEADER = new byte[]
			{
					033, (byte) 'L', (byte) 'u', (byte) 'a',
					0x51, 0, 99, 4,
					4, 4, 8, 0};

	/**
	 * Loads and checks the binary chunk header.  Sets
	 * <code>this.bigendian</code> accordingly.
	 * <p>
	 * A Lua 5.1 header looks like this:
	 * <pre>
	 * b[0]    0x33
	 * b[1..3] "Lua";
	 * b[4]    0x51 (LUAC_VERSION)
	 * b[5]    0 (LUAC_FORMAT)
	 * b[6]    0 big-endian, 1 little-endian
	 * b[7]    4 (sizeof(int))
	 * b[8]    4 (sizeof(size_t))
	 * b[9]    4 (sizeof(Instruction))
	 * b[10]   8 (sizeof(lua_Number))
	 * b[11]   0 (floating point)
	 * </pre>
	 * <p>
	 * To conserve JVM bytecodes the sizes of the types <code>int</code>,
	 * <code>size_t</code>, <code>Instruction</code>,
	 * <code>lua_Number</code> are assumed by the code to be 4, 4, 4, and
	 * 8, respectively.  Where this assumption is made the tags :int:size,
	 * :size_t:size :Instruction:size :lua_Number:size will appear so that
	 * you can grep for them, should you wish to modify this loader to
	 * load binary chunks from different architectures.
	 *
	 * @throws IOException when header is malformed or not suitable.
	 */
	private void header() throws IOException {
		byte[] buf = new byte[HEADER_SIZE];
		read(buf);

		// poke the HEADER's endianness byte and compare.
		byte byteOrderMark = buf[6];
		HEADER[6] = byteOrderMark;
		if (byteOrderMark < 0 || byteOrderMark > 1 || !arrayEquals(HEADER, buf))
			throw new IOException();
		bigendian = byteOrderMark == 0;
	}

	/**
	 * Undumps a Lua number.  Which is assumed to be a 64-bit IEEE double.
	 */
	private double number() throws IOException {
		// :lua_Number:size  Here we assume that the size is 8.
		read(longbuf);
		// Big-endian architectures store doubles with the sign bit first;
		// little-endian is the other way around.
		long l = 0;
		for (int i = 0; i < 8; ++i) {
			if (bigendian)
				l = (l << 8) | (longbuf[i] & 0xff);
			else
				l = (l >>> 8) | (((long) (longbuf[i] & 0xff)) << 56);
		}
		return Double.longBitsToDouble(l);
	}

	/**
	 * Undumps the <code>Proto</code> array contained inside a
	 * <code>Proto</code> object.  These are the <code>Proto</code>
	 * objects for all inner functions defined inside an existing
	 * function.  Corresponds to the second half of PUC-Rio's
	 * <code>LoadConstants</code> function.  See <code>constant</code> for
	 * the first half.
	 */
	private Proto[] proto(String source) throws IOException {
		int n = readInt();
		Proto[] p = new Proto[n];
		for (int i = 0; i < n; ++i)
			p[i] = readFunction(source);
		return p;
	}

	/**
	 * Undumps a {@link String} or <code>null</code>.
	 * As per <code>LoadString</code> in PUC-Rio's lundump.c.
	 * Strings are converted from the binary using the UTF-8 encoding,
	 * using the {@link java.lang.String#String(byte[], java.nio.charset.Charset) String(byte[], java.nio.charset.Charset)} constructor.
	 */
	private String readString() throws IOException {
		// :size_t:size we assume that size_t is same size as int.
		int size = readInt();
		if (size == 0)
			return null;
		byte[] buf = new byte[size - 1];
		read(buf);
		// Discard trailing NUL byte
		if (in.read() == -1)
			throw new EOFException();
		return new String(buf, StandardCharsets.UTF_8).intern();
	}

	/**
	 * CLDC 1.1 does not provide <code>java.util.Arrays</code> so we make
	 * do with this.
	 */
	private static boolean arrayEquals(byte[] x, byte[] y) {
		int length = x.length;
		if (length != y.length)
			return false;
		for (int i = 0; i < length; ++i) {
			if (x[i] != y[i])
				return false;
		}
		return true;
	}
}


