package me.jezza.lava.runtime;

import me.jezza.lava.runtime.Interpreter.Ops;

/**
 * @author Jezza
 */
public final class ByteCodeWriter {
	private static final int GROWTH_RATE = 64;

	private byte[] data;
	private int index;

	public ByteCodeWriter(int size) {
		data = new byte[size];
		index = 0;
	}

	public byte get(int index) {
		return data[index];
	}

	private void check(int length) {
		if (index + length >= data.length) {
			int l = data.length;
			byte[] newData = new byte[l + GROWTH_RATE];
			System.arraycopy(data, 0, newData, 0, l);
			data = newData;
		}
	}

	public void write1(int code) {
		check(1);
		data[index++] = (byte) (code);
	}

	public void write2(int code) {
		check(2);
		data[index++] = (byte) (code >> 8);
		data[index++] = (byte) (code);
	}

	public void write4(int code) {
		check(4);
		int i = index;
		data[i++] = (byte) (code >> 24);
		data[i++] = (byte) (code >> 16);
		data[i++] = (byte) (code >> 8);
		data[i++] = (byte) (code);
		index = i;
	}

	public void write1(Ops code) {
		write1(code.ordinal());
	}

	public void write1(Ops code, int second) {
		write1(code.ordinal());
		write1(second);
	}

	public void write2(Ops code, int second) {
		write1(code.ordinal());
		write2(second);
	}

	public void write4(Ops code, int second) {
		write1(code.ordinal());
		write4(second);
	}

	public void write1(Ops code, int second, int third) {
		write1(code.ordinal());
		write1(second);
		write1(third);
	}

	public void write2(Ops code, int second, int third) {
		write1(code.ordinal());
		write2(second);
		write2(third);
	}

	public void write4(Ops code, int second, int third) {
		write1(code.ordinal());
		write4(second);
		write4(third);
	}

	public int mark() {
		return index;
	}

	public void patch1(int index, int code) {
		int old = this.index;
		this.index = index;
		write1(code);
		this.index = old;
	}

	public void patch2(int index, int code) {
		int old = this.index;
		this.index = index;
		write2(code);
		this.index = old;
	}

	public void patch4(int index, int code) {
		int old = this.index;
		this.index = index;
		write4(code);
		this.index = old;
	}

	public byte[] code() {
		byte[] code = new byte[index];
		System.arraycopy(data, 0, code, 0, index);
		return code;
	}
}
