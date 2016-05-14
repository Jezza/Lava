package me.jezza.lava;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Jezza
 */
final class DumpWriter extends DataOutputStream {

	/**
	 * Creates a new data output stream to write data to the specified
	 * underlying output stream. The counter <code>written</code> is
	 * set to zero.
	 *
	 * @param out the underlying output stream, to be saved for later
	 *            use.
	 * @see FilterOutputStream#out
	 */
	DumpWriter(OutputStream out) {
		super(out);
	}

	//////////////// dumper ////////////////////

	/**
	 * In order to make the code more compact the dumper re-uses the
	 * header defined in Loader.java.  It has to fix the endianness byte
	 * first.
	 */
	void dumpHeader() throws IOException {
		Loader.HEADER[6] = 0;
		write(Loader.HEADER);
	}

	void dumpFunction(Proto f, String p) throws IOException {
		String source = f.source;
		writeString(source != null && source.equals(p) ? source : null);
		writeInt(f.linedefined);
		writeInt(f.lastlinedefined);
		writeByte(f.nups);
		writeByte(f.numparams);
		writeBoolean(f.isVararg());
		writeByte(f.maxstacksize);
		dumpCode(f);
		dumpConstants(f);
		dumpDebug(f);
	}

	private void dumpCode(Proto f) throws IOException {
		int[] code = f.code;
		int n = code.length;
		writeInt(n);
		for (int c : code)
			writeInt(c);
	}

	private void dumpConstants(Proto f) throws IOException {
		Slot[] constants = f.constants();
		int n = constants.length;
		writeInt(n);
		for (int i = 0; i < n; i++) {
			Slot constant = constants[i];
			Object o = constant.r;
			if (o == Lua.BYPASS_TYPE) {
				switch (constant.t) {
					case Lua.TBOOLEAN:
						writeByte(Lua.TBOOLEAN);
						writeBoolean((Boolean) o);
						break;
					case Lua.TNUMBER:
						writeByte(Lua.TNUMBER);
						writeDouble(constant.d);
						break;
					default:
						throw new UnsupportedOperationException("Slot Type not yet implemented yet: " + Lua.typeName(constant.t));
				}
			} else {
				if (o == Lua.NIL) {
					writeByte(Lua.TNIL);
				} else if (o instanceof String) {
					writeByte(Lua.TSTRING);
					writeString((String) o);
				} else {
					//# assert false
				}
			}
		}
		Proto[] p = f.p;
		n = p.length;
		writeInt(n);
		for (int i = 0; i < n; i++)
			dumpFunction(p[i], f.source);
	}

	/**
	 * Strings are dumped by converting to UTF-8 encoding.  The MIDP
	 * 2.0 spec guarantees that this encoding will be supported (see
	 * page 9 of midp-2_0-fr-spec.pdf).  Nonetheless, any
	 * possible UnsupportedEncodingException is left to be thrown
	 * (it's a subclass of IOException which is declared to be thrown).
	 */
	private void writeString(String s) throws IOException {
		if (s == null) {
			writeInt(0);
		} else {
			byte[] contents = s.getBytes(UTF_8);
			int size = contents.length;
			writeInt(size + 1);
			write(contents, 0, size);
			writeByte(0);
		}
	}

	private void dumpDebug(Proto f) throws IOException {
		int[] lineinfo = f.lineinfo;
		int n = lineinfo.length;
		writeInt(n);
		for (int i = 0; i < n; i++)
			writeInt(lineinfo[i]);

		LocVar[] locvars = f.locvars;
		n = locvars.length;
		writeInt(n);
		for (int i = 0; i < n; i++) {
			LocVar locvar = locvars[i];
			writeString(locvar.varname);
			writeInt(locvar.startpc);
			writeInt(locvar.endpc);
		}

		String[] upvalues = f.upvalues;
		n = upvalues.length;
		writeInt(n);
		for (int i = 0; i < n; i++)
			writeString(upvalues[i]);
	}

	static void proto(OutputStream writer, Proto f) throws IOException {
		DumpWriter d = new DumpWriter(writer);
		d.dumpHeader();
		d.dumpFunction(f, null);
		d.flush();
	}
}
