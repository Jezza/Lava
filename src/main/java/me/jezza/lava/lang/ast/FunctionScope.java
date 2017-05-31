package me.jezza.lava.lang.ast;

/**
 * @author Jezza
 */
public final class FunctionScope {

	public static final int VARARGS = 1;

	private final FunctionScope previous;

	private int flags;

	public FunctionScope(FunctionScope previous) {

		this.previous = previous;
	}

	public void set(int flags, boolean set) {
		if (set) {
			this.flags |= flags;
		} else {
			this.flags &= ~flags;
		}
	}

	public boolean is(int flags) {
		return (this.flags & flags) == flags;
	}

}
