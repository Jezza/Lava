package me.jezza.lava.lang.util;

/**
 * @author Jezza
 */
public final class ByteCodeWriter {
	private static final int GROWTH_RATE = 64;

	private byte[] data;
	private int index;

	public ByteCodeWriter() {
		this(64);
	}

	public ByteCodeWriter(int initialCapacity) {
		data = new byte[initialCapacity];
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

	public void write1(int code, int second) {
		write1(code);
		write1(second);
	}

	public void write2(int code, int second) {
		write1(code);
		write2(second);
	}

	public void write4(int code, int second) {
		write1(code);
		write4(second);
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

	public void write4(int code, int second, int third) {
		write1(code);
		write4(second);
		write4(third);
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

	public void write4(int code, int second, int third, int fourth) {
		write1(code);
		write4(second);
		write4(third);
		write4(fourth);
	}

	public int mark() {
		return index;
	}
	
	public int mark2() {
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

	public void patch4(int index, int code) {
		int old = this.index;
		this.index = index;
		write4(code);
		this.index = old;
	}

	public void backPatch1(int index) {
		int old = this.index;
		this.index = index;
		write1(old);
		this.index = old;
	}

	public void backPatch2(int index) {
		int old = this.index;
		this.index = index;
		write2(old);
		this.index = old;
	}

	public void backPatch4(int index) {
		int old = this.index;
		this.index = index;
		write4(old);
		this.index = old;
	}

	public byte[] code() {
		byte[] code = new byte[index];
		System.arraycopy(data, 0, code, 0, index);
		return code;
	}
}
