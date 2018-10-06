package me.jezza.lava.lang.util;

import java.util.Arrays;

/**
 * @author Jezza
 */
public final class ByteCodeWriter {
	private static final int GROWTH_RATE = 64;

	private byte[] data;
	private int index;

	public ByteCodeWriter() {
		this(GROWTH_RATE);
	}

	public ByteCodeWriter(int initialCapacity) {
		data = new byte[initialCapacity];
		index = 0;
	}

	public byte get(int index) {
		return data[index];
	}

	public int get2(int index) {
		int value = (get(index) & 0xFF) << 8
				| (get(index + 1) & 0xFF);
		// @TODO Jezza - 06 Oct. 2018: I don't like the check here...
		// Is there a better way to represent no value with 2 bytes?
		return value == 0xFF_FF
				? -1
				: value;
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

	public void write1(int code, int second) {
		write1(code);
		write1(second);
	}

	public void write2(int code, int second) {
		write1(code);
		write2(second);
	}

	public void write1(int code, int second, int third) {
		write1(code);
		write1(second);
		write1(third);
	}

	public void write2(int code, int second, int third) {
		write1(code);
		write2(second);
		write2(third);
	}

	public void write1(int code, int second, int third, int fourth) {
		write1(code);
		write1(second);
		write1(third);
		write1(fourth);
	}

	public void write2(int code, int second, int third, int fourth) {
		write1(code);
		write2(second);
		write2(third);
		write2(fourth);
	}

	public void write1(int code, int second, int third, int fourth, int fifth) {
		write1(code);
		write1(second);
		write1(third);
		write1(fourth);
		write1(fifth);
	}

	public void write2(int code, int second, int third, int fourth, int fifth) {
		write1(code);
		write2(second);
		write2(third);
		write2(fourth);
		write2(fifth);
	}

	public int mark() {
		return index;
	}

	public int reserve2() {
		int index = mark();
		write2(Integer.MAX_VALUE);
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

	public void patchToHere1(int index) {
		int old = this.index;
		this.index = index;
		write1(old);
		this.index = old;
	}

	public void patchToHere2(int index) {
		int old = this.index;
		this.index = index;
		write2(old);
		this.index = old;
	}

	public byte[] code() {
		return Arrays.copyOf(data, index);
	}
}
