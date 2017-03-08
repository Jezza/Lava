package me.jezza.lava;

/**
 * @author Jezza
 */
public class Context {

	private final Lua L;

	public Context(Lua L) {
		this.L = L;
	}

	public void enterLevel() {
		L.nCcalls++;
	}

	public void leaveLevel() {
		L.nCcalls--;
	}

}
