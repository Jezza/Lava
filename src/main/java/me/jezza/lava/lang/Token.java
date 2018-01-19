package me.jezza.lava.lang;

/**
 * @author Jezza
 */
public final class Token {
	public static final Token EOS = new Token(Tokens.EOS, -1, -1, "");

	public final int type;
	public final int row;
	public final int col;

	// @TODO Jezza - 03 Dec 2017: Numbers....
	public final String text;

	public Token(int type, int row, int col, String text) {
		this.type = type;
		this.row = row;
		this.col = col;
		this.text = text;
	}

	@Override
	public String toString() {
		return "Token{" + Tokens.name(type) + ", \"" + text + "\", [" + row + "," + col + "->" + (col + text.length()) + ")}";
	}
}